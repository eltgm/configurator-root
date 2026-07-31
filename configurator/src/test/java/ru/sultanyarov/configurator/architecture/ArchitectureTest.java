package ru.sultanyarov.configurator.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "ru.sultanyarov.configurator",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule CONTROLLERS_USE_FACADES_INSTEAD_OF_LOWER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..api.inbounds.rest.controller..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..application.service..", "..application.port..", "..infrastructure..");

  @ArchTest
  static final ArchRule FACADES_DO_NOT_BYPASS_SERVICES =
      noClasses()
          .that()
          .resideInAPackage("..application.facade..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..application.port..", "..infrastructure..");

  @ArchTest
  static final ArchRule SERVICES_ARE_INDEPENDENT_OF_TRANSPORT_AND_INFRASTRUCTURE =
      noClasses()
          .that()
          .resideInAPackage("..application.service..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "..infrastructure..");

  @ArchTest
  static final ArchRule DOMAIN_IS_INDEPENDENT_OF_OUTER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..domain.model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "..application..", "..infrastructure..");
}
