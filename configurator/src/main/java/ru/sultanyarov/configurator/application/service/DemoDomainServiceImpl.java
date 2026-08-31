package ru.sultanyarov.configurator.application.service;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CurrentUserProvider;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.model.Domain;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDomainServiceImpl implements DemoDomainService {
  static final String DEMO_DOMAIN_NAME = "Сборка ПК";
  static final String DEMO_CONFIGURATION_NAME = "Игровой ПК 1440p";

  private final DomainService domainService;
  private final ComponentTypeService componentTypeService;
  private final AttributeService attributeService;
  private final ComponentService componentService;
  private final CompatibilityService compatibilityService;
  private final CompatibilityRuleService compatibilityRuleService;
  private final ConfigurationService configurationService;
  private final CurrentUserProvider currentUserProvider;

  @Override
  @Transactional
  public Domain createDemoDomain() {
    log.info("Creating demo PC domain");
    Domain domain = createDomain();
    DemoTypes types = createTypes(domain.id());
    DemoAttributes attributes = createAttributes(types);
    DemoComponents components = createComponents(types, attributes);
    createAutomaticRules(domain.id(), types, attributes);
    createManualLinks(domain.id(), components);
    createConfiguration(domain.id(), components);
    return domain;
  }

  private Domain createDomain() {
    return domainService.create(
        Domain.builder()
            .name(DEMO_DOMAIN_NAME)
            .description(
                "Демонстрационный каталог для сборки игрового ПК с ручной и автоматической совместимостью")
            .createdByUserId(currentUserProvider.getCurrentUserId())
            .componentTypes(List.of())
            .build());
  }

  private DemoTypes createTypes(Long domainId) {
    return new DemoTypes(
        createType(domainId, "Процессор", "CPU", "Центральные процессоры", 0),
        createType(domainId, "Материнская плата", "MOTHERBOARD", "Материнские платы", 1),
        createType(domainId, "Оперативная память", "MEMORY", "Модули оперативной памяти", 2),
        createType(domainId, "Видеокарта", "GPU", "Дискретные видеокарты", 3),
        createType(domainId, "Блок питания", "PSU", "Блоки питания", 4),
        createType(domainId, "Корпус", "CASE", "Корпуса для компьютера", 5));
  }

  private ComponentType createType(
      Long domainId, String name, String code, String description, int orderIndex) {
    return componentTypeService.create(
        ComponentType.builder()
            .domainId(domainId)
            .name(name)
            .code(code)
            .description(description)
            .orderIndex(orderIndex)
            .build());
  }

  private DemoAttributes createAttributes(DemoTypes types) {
    AttributeDefinition cpuSocket =
        createEnumAttribute(types.cpu(), "socket", "Сокет", Set.of("AM5", "LGA1700"), 0);
    AttributeDefinition cpuTdp = createNumberAttribute(types.cpu(), "tdp", "Теплопакет, Вт", 1);

    AttributeDefinition motherboardSocket = attachAttribute(types.motherboard(), cpuSocket, 0);
    AttributeDefinition motherboardMemory =
        createEnumAttribute(
            types.motherboard(), "memory_standard", "Стандарт памяти", Set.of("DDR4", "DDR5"), 1);
    AttributeDefinition motherboardFormFactor =
        createEnumAttribute(
            types.motherboard(), "form_factor", "Форм-фактор", Set.of("ATX", "MICRO_ATX"), 2);

    AttributeDefinition memoryStandard = attachAttribute(types.memory(), motherboardMemory, 0);
    AttributeDefinition memoryCapacity =
        createNumberAttribute(types.memory(), "capacity_gb", "Объём, ГБ", 1);

    AttributeDefinition gpuRecommendedPower =
        createNumberAttribute(types.gpu(), "recommended_power", "Рекомендуемая мощность БП, Вт", 0);
    AttributeDefinition gpuLength = createNumberAttribute(types.gpu(), "length_mm", "Длина, мм", 1);

    AttributeDefinition psuPower = createNumberAttribute(types.psu(), "power", "Мощность, Вт", 0);

    AttributeDefinition caseFormFactor = attachAttribute(types.pcCase(), motherboardFormFactor, 0);
    AttributeDefinition caseMaxGpuLength =
        createNumberAttribute(
            types.pcCase(), "max_gpu_length_mm", "Максимальная длина видеокарты, мм", 1);

    return new DemoAttributes(
        cpuSocket,
        cpuTdp,
        motherboardSocket,
        motherboardMemory,
        motherboardFormFactor,
        memoryStandard,
        memoryCapacity,
        gpuRecommendedPower,
        gpuLength,
        psuPower,
        caseFormFactor,
        caseMaxGpuLength);
  }

  private AttributeDefinition createEnumAttribute(
      ComponentType type, String name, String label, Set<String> enumValues, int orderIndex) {
    return createAttribute(type, name, label, DataType.ENUM, enumValues, orderIndex);
  }

  private AttributeDefinition createNumberAttribute(
      ComponentType type, String name, String label, int orderIndex) {
    return createAttribute(type, name, label, DataType.NUMBER, Set.of(), orderIndex);
  }

  private AttributeDefinition createAttribute(
      ComponentType type,
      String name,
      String label,
      DataType dataType,
      Set<String> enumValues,
      int orderIndex) {
    AttributeDefinition definition =
        attributeService.createInDomain(
            type.domainId(),
            AttributeDefinition.builder()
                .domainId(type.domainId())
                .name(name)
                .label(label)
                .dataType(dataType)
                .enumValues(enumValues)
                .build());
    return attachAttribute(type, definition, orderIndex);
  }

  private AttributeDefinition attachAttribute(
      ComponentType type, AttributeDefinition definition, int orderIndex) {
    return attributeService.attachToComponentType(type.id(), definition.id(), true, orderIndex);
  }

  private DemoComponents createComponents(DemoTypes types, DemoAttributes attributes) {
    Component ryzen =
        createComponent(
            types.cpu(),
            "Ryzen 5 7600",
            "AMD",
            "Шестиядерный процессор для платформы AM5",
            value(attributes.cpuSocket(), "AM5"),
            value(attributes.cpuTdp(), "65"));
    Component intel =
        createComponent(
            types.cpu(),
            "Core i5-14600K",
            "Intel",
            "Процессор для платформы LGA1700",
            value(attributes.cpuSocket(), "LGA1700"),
            value(attributes.cpuTdp(), "125"));

    Component asusMotherboard =
        createComponent(
            types.motherboard(),
            "TUF GAMING B650-PLUS",
            "ASUS",
            "ATX-плата AM5 с поддержкой DDR5",
            value(attributes.motherboardSocket(), "AM5"),
            value(attributes.motherboardMemory(), "DDR5"),
            value(attributes.motherboardFormFactor(), "ATX"));
    Component msiMotherboard =
        createComponent(
            types.motherboard(),
            "PRO B760M-P DDR4",
            "MSI",
            "Micro-ATX плата LGA1700 с поддержкой DDR4",
            value(attributes.motherboardSocket(), "LGA1700"),
            value(attributes.motherboardMemory(), "DDR4"),
            value(attributes.motherboardFormFactor(), "MICRO_ATX"));

    Component ddr5Memory =
        createComponent(
            types.memory(),
            "FURY Beast 32GB DDR5-6000",
            "Kingston",
            "Комплект DDR5 объёмом 32 ГБ",
            value(attributes.memoryStandard(), "DDR5"),
            value(attributes.memoryCapacity(), "32"));
    Component ddr4Memory =
        createComponent(
            types.memory(),
            "Vengeance LPX 32GB DDR4-3600",
            "Corsair",
            "Комплект DDR4 объёмом 32 ГБ",
            value(attributes.memoryStandard(), "DDR4"),
            value(attributes.memoryCapacity(), "32"));

    Component rtx =
        createComponent(
            types.gpu(),
            "GeForce RTX 4070 SUPER",
            "NVIDIA",
            "Видеокарта для игр в разрешении 1440p",
            value(attributes.gpuRecommendedPower(), "650"),
            value(attributes.gpuLength(), "267"));
    Component radeon =
        createComponent(
            types.gpu(),
            "Radeon RX 7800 XT",
            "AMD",
            "Видеокарта с рекомендуемым блоком питания 700 Вт",
            value(attributes.gpuRecommendedPower(), "700"),
            value(attributes.gpuLength(), "280"));

    Component corsairPsu =
        createComponent(
            types.psu(),
            "RM750e",
            "Corsair",
            "Модульный блок питания мощностью 750 Вт",
            value(attributes.psuPower(), "750"));
    Component beQuietPsu =
        createComponent(
            types.psu(),
            "PicoPSU-90",
            "Mini-Box",
            "Компактный блок питания мощностью 90 Вт для маломощных систем",
            value(attributes.psuPower(), "90"));

    Component fractalCase =
        createComponent(
            types.pcCase(),
            "Pop Air",
            "Fractal Design",
            "ATX-корпус с запасом места для видеокарты",
            value(attributes.caseFormFactor(), "ATX"),
            value(attributes.caseMaxGpuLength(), "405"));
    Component compactCase =
        createComponent(
            types.pcCase(),
            "MasterBox Q300L Compact",
            "Cooler Master",
            "Компактный Micro-ATX корпус с ограничением видеокарты 260 мм",
            value(attributes.caseFormFactor(), "MICRO_ATX"),
            value(attributes.caseMaxGpuLength(), "260"));

    return new DemoComponents(
        ryzen,
        intel,
        asusMotherboard,
        msiMotherboard,
        ddr5Memory,
        ddr4Memory,
        rtx,
        radeon,
        corsairPsu,
        beQuietPsu,
        fractalCase,
        compactCase);
  }

  private Component createComponent(
      ComponentType type,
      String name,
      String brand,
      String description,
      AttributeValue... attributes) {
    return componentService.create(
        Component.builder()
            .componentTypeId(type.id())
            .name(name)
            .brand(brand)
            .description(description)
            .archived(false)
            .attributes(List.of(attributes))
            .images(List.of())
            .build());
  }

  private static AttributeValue value(AttributeDefinition definition, String value) {
    return AttributeValue.builder().attributeDefinitionId(definition.id()).value(value).build();
  }

  private void createAutomaticRules(Long domainId, DemoTypes types, DemoAttributes attributes) {
    createRule(
        domainId,
        "Совпадение сокета процессора и материнской платы",
        types.cpu(),
        types.motherboard(),
        attributes.cpuSocket(),
        CompatibilityRuleOperator.EQUALS,
        attributes.motherboardSocket());
    createRule(
        domainId,
        "Совпадение стандарта памяти",
        types.motherboard(),
        types.memory(),
        attributes.motherboardMemory(),
        CompatibilityRuleOperator.EQUALS,
        attributes.memoryStandard());
    createRule(
        domainId,
        "Соответствие форм-фактора корпуса",
        types.motherboard(),
        types.pcCase(),
        attributes.motherboardFormFactor(),
        CompatibilityRuleOperator.EQUALS,
        attributes.caseFormFactor());
    createRule(
        domainId,
        "Достаточная мощность блока питания",
        types.gpu(),
        types.psu(),
        attributes.gpuRecommendedPower(),
        CompatibilityRuleOperator.LTE,
        attributes.psuPower());
    createRule(
        domainId,
        "Мощность блока питания не ниже TDP процессора",
        types.cpu(),
        types.psu(),
        attributes.cpuTdp(),
        CompatibilityRuleOperator.LTE,
        attributes.psuPower());
    createRule(
        domainId,
        "Допустимая длина видеокарты",
        types.gpu(),
        types.pcCase(),
        attributes.gpuLength(),
        CompatibilityRuleOperator.LTE,
        attributes.caseMaxGpuLength());
  }

  private void createRule(
      Long domainId,
      String name,
      ComponentType leftType,
      ComponentType rightType,
      AttributeDefinition leftAttribute,
      CompatibilityRuleOperator operator,
      AttributeDefinition rightAttribute) {
    compatibilityRuleService.create(
        CompatibilityRuleSet.builder()
            .domainId(domainId)
            .name(name)
            .componentTypeAId(leftType.id())
            .componentTypeBId(rightType.id())
            .enabled(true)
            .conditions(
                List.of(
                    CompatibilityRuleCondition.builder()
                        .leftAttributeDefinitionId(leftAttribute.id())
                        .operator(operator)
                        .rightAttributeDefinitionId(rightAttribute.id())
                        .orderIndex(0)
                        .build()))
            .build());
  }

  private void createManualLinks(Long domainId, DemoComponents components) {
    // PCI Express support is represented manually; all other demo relations are rule-driven.
    createLink(domainId, components.asusMotherboard(), components.rtx());
    createLink(domainId, components.msiMotherboard(), components.radeon());
  }

  private void createLink(Long domainId, Component first, Component second) {
    compatibilityService.create(
        CompatibilityLink.builder()
            .domainId(domainId)
            .componentAId(first.getId())
            .componentBId(second.getId())
            .comment("Совместимость слота PCI Express")
            .build());
  }

  private void createConfiguration(Long domainId, DemoComponents components) {
    configurationService.create(
        domainId,
        new ConfigurationDraft(
            DEMO_CONFIGURATION_NAME,
            "Готовая совместимая конфигурация для игр в разрешении 1440p",
            List.of(
                components.ryzen().getId(),
                components.asusMotherboard().getId(),
                components.ddr5Memory().getId(),
                components.rtx().getId(),
                components.corsairPsu().getId(),
                components.fractalCase().getId())));
  }

  private record DemoTypes(
      ComponentType cpu,
      ComponentType motherboard,
      ComponentType memory,
      ComponentType gpu,
      ComponentType psu,
      ComponentType pcCase) {}

  private record DemoAttributes(
      AttributeDefinition cpuSocket,
      AttributeDefinition cpuTdp,
      AttributeDefinition motherboardSocket,
      AttributeDefinition motherboardMemory,
      AttributeDefinition motherboardFormFactor,
      AttributeDefinition memoryStandard,
      AttributeDefinition memoryCapacity,
      AttributeDefinition gpuRecommendedPower,
      AttributeDefinition gpuLength,
      AttributeDefinition psuPower,
      AttributeDefinition caseFormFactor,
      AttributeDefinition caseMaxGpuLength) {}

  private record DemoComponents(
      Component ryzen,
      Component intel,
      Component asusMotherboard,
      Component msiMotherboard,
      Component ddr5Memory,
      Component ddr4Memory,
      Component rtx,
      Component radeon,
      Component corsairPsu,
      Component beQuietPsu,
      Component fractalCase,
      Component compactCase) {}
}
