package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.AttributeValueRepository;
import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeValueServiceImplTest {

    @Mock
    private AttributeValueRepository attributeValueRepository;

    @InjectMocks
    private AttributeValueServiceImpl attributeValueService;

    @Test
    void createAttributeValues_shouldDelegateToRepository() {
        List<AttributeValue> values = List.of(AttributeValue.builder().attributeDefinitionId(1L).value("value").build());
        List<AttributeValue> createdValues = List.of(AttributeValue.builder().id(10L).attributeDefinitionId(1L).value("value").build());

        when(attributeValueRepository.createAttributeValues(values, 7L)).thenReturn(createdValues);

        List<AttributeValue> result = attributeValueService.createAttributeValues(values, 7L);

        assertThat(result).isEqualTo(createdValues);
        verify(attributeValueRepository).createAttributeValues(values, 7L);
    }
}
