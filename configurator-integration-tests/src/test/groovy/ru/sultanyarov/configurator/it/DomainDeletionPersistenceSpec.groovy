package ru.sultanyarov.configurator.it

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import ru.sultanyarov.configurator.ConfiguratorApplication
import ru.sultanyarov.configurator.application.port.out.ComponentImageCleanupRepository
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage
import ru.sultanyarov.configurator.application.service.ComponentImageCleanupService
import ru.sultanyarov.configurator.application.service.DomainService
import ru.sultanyarov.configurator.domain.exception.DomainHasConfigurationsException
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles('test')
@SpringBootTest(classes = ConfiguratorApplication, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DomainDeletionPersistenceSpec extends Specification {
    @Autowired DataSource dataSource
    @Autowired PlatformTransactionManager transactionManager
    @Autowired DomainService domainService
    @Autowired ComponentImageCleanupRepository cleanupRepository

    JdbcTemplate jdbc
    def executor = Executors.newFixedThreadPool(2)
    def release = new CountDownLatch(1)
    static final TABLES = ['domain', 'component_type', 'attribute_definition', 'component_type_attribute',
                          'component', 'attribute_value', 'component_image', 'compatibility_link',
                          'compatibility_rule_set', 'compatibility_rule_condition', 'configuration',
                          'configuration_component', 'component_image_cleanup']

    def setup() {
        jdbc = new JdbcTemplate(dataSource)
        def scripts = ['sql/clear-db.sql', 'sql/insert-configurator-test-data.sql', 'sql/insert-configurator-image-data.sql']
        new ResourceDatabasePopulator(scripts.collect { new ClassPathResource(it) } as ClassPathResource[]).execute(dataSource)
    }

    def cleanup() {
        release.countDown()
        executor.shutdownNow()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }

    def 'rolls back deleted children and queued images if a later database operation fails'() {
        given: 'an inconsistent cross-domain reference forces a FK failure after cleanup has been queued'
        jdbc.update("INSERT INTO configuration (id, domain_id, name, created_by_user_id) VALUES (950, 2, 'Cross-domain reference', -1)")
        jdbc.update('INSERT INTO configuration_component (configuration_id, component_id) VALUES (950, 1)')
        def before = snapshot()

        when:
        domainService.deleteById(1L)

        then:
        def error = thrown(RuntimeException)
        error.message.contains('fk_configuration_component_component')
        snapshot() == before
        cleanupRepository.findDue(Instant.now().plusSeconds(1), 100).empty
    }

    def 'durable cleanup survives storage outages and worker recreation'() {
        given:
        ComponentImageStorage restored = Mock()
        def imageKeys = jdbc.queryForList('SELECT file_path FROM component_image', String)
        domainService.deleteById(1L)
        ComponentImageStorage offline = Stub() {
            delete(_) >> { throw new ExternalStorageException(new IOException('offline'), 'offline') }
        }

        when:
        new ComponentImageCleanupService(cleanupRepository, offline).cleanUpPendingImages()

        then:
        jdbc.queryForObject('SELECT count(*) FROM domain WHERE id = 1', Integer) == 0
        jdbc.queryForObject('SELECT count(*) FROM component_image', Integer) == 0
        jdbc.queryForList('SELECT attempts FROM component_image_cleanup', Integer).every { it == 1 }
        cleanupRepository.findDue(Instant.now(), 100).empty
        cleanupRepository.findDue(Instant.now().plusSeconds(120), 100).toSet() == imageKeys.toSet()

        when:
        jdbc.update("UPDATE component_image_cleanup SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'")
        def restartedRepository = cleanupRepository
        new ComponentImageCleanupService(restartedRepository, restored).cleanUpPendingImages()

        then:
        imageKeys.size() * restored.delete({ it in imageKeys })
        jdbc.queryForObject('SELECT count(*) FROM component_image_cleanup', Integer) == 0
    }

    def 'waits for an in-flight configuration insert and then rejects deletion without changes'() {
        given:
        def inserted = new CountDownLatch(1)
        def writer = transactionAsync {
            jdbc.update("INSERT INTO configuration (id, domain_id, name, created_by_user_id) VALUES (950, 1, 'Concurrent build', -1)")
            inserted.countDown()
            assert release.await(10, TimeUnit.SECONDS)
        }
        assert inserted.await(5, TimeUnit.SECONDS)
        def before = snapshot()

        when:
        def deletion = async { domainService.deleteById(1L) }
        waitUntilBlocked()
        release.countDown()

        then:
        writer.get(5, TimeUnit.SECONDS) == null
        deletion.get(5, TimeUnit.SECONDS) instanceof DomainHasConfigurationsException
        snapshot().findAll { it.key != 'configuration' } == before.findAll { it.key != 'configuration' }
        jdbc.queryForObject('SELECT count(*) FROM configuration WHERE id = 950', Integer) == 1
    }

    def 'a configuration insert cannot slip past a successful domain deletion and jobs stay invisible until commit'() {
        given:
        def deleted = new CountDownLatch(1)
        def deletion = transactionAsync {
            domainService.deleteById(1L)
            deleted.countDown()
            assert release.await(10, TimeUnit.SECONDS)
        }
        assert deleted.await(5, TimeUnit.SECONDS)
        assert cleanupRepository.findDue(Instant.now().plusSeconds(1), 100).empty

        when:
        def writer = async {
            jdbc.update("INSERT INTO configuration (id, domain_id, name, created_by_user_id) VALUES (950, 1, 'Too late', -1)")
        }
        waitUntilBlocked()
        release.countDown()

        then:
        deletion.get(5, TimeUnit.SECONDS) == null
        writer.get(5, TimeUnit.SECONDS) instanceof org.springframework.dao.DataIntegrityViolationException
        jdbc.queryForObject('SELECT count(*) FROM configuration', Integer) == 0
        jdbc.queryForObject('SELECT count(*) FROM domain WHERE id = 1', Integer) == 0
        cleanupRepository.findDue(Instant.now().plusSeconds(1), 100).size() == 5
    }

    def 'includes images committed during deletion before acquiring the component lock'() {
        given:
        def inserted = new CountDownLatch(1)
        def writer = transactionAsync {
            jdbc.update("INSERT INTO component_image (component_id, file_path) VALUES (1, 'components/1/concurrent.png')")
            inserted.countDown()
            assert release.await(10, TimeUnit.SECONDS)
        }
        assert inserted.await(5, TimeUnit.SECONDS)

        when:
        def deletion = async { domainService.deleteById(1L) }
        waitUntilBlocked()
        release.countDown()

        then:
        writer.get(5, TimeUnit.SECONDS) == null
        deletion.get(5, TimeUnit.SECONDS) == null
        cleanupRepository.findDue(Instant.now().plusSeconds(1), 100).contains('components/1/concurrent.png')
        jdbc.queryForObject('SELECT count(*) FROM component WHERE id <> 7', Integer) == 0
    }

    private Map snapshot() {
        TABLES.collectEntries { [(it): jdbc.queryForList('SELECT * FROM ' + it + ' ORDER BY 1')] }
    }

    private void waitUntilBlocked() {
        new PollingConditions(timeout: 5, delay: 0.01).eventually {
            assert jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'", Integer) > 0
        }
    }

    private def transactionAsync(Closure action) {
        async { new TransactionTemplate(transactionManager).executeWithoutResult { action() } }
    }

    private def async(Closure action) {
        executor.submit({
            try { action(); return null } catch (Exception exception) { return exception }
        } as Callable<Exception>)
    }
}
