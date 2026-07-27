/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit enforcement of the volatility-based layering (IDesign / ADR-029, ADR-032). These
 * rules make the architecture self-checking, so the violations that had to be caught by eye
 * this session fail the build instead. app-batch sees the whole production graph
 * (assembly -> variants -> impls -> contracts), so the suite runs here over main classes only.
 */
@AnalyzeClasses(packages = "systems.asymm.cipn", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule application_depends_only_on_wiring_and_contracts = noClasses()
            .that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAPackage("..svc..")
            .because("the application must go through the wiring assembly, never impl (svc-*) modules");

    @ArchTest
    static final ArchRule manager_does_not_reach_other_service_impls = noClasses()
            .that().resideInAPackage("..svc.manager..")
            .should().dependOnClassesThat().resideInAnyPackage("..svc.engine..", "..svc.accessor..")
            .because("a manager orchestrates via contracts; engine/accessor impls are wired externally");

    @ArchTest
    static final ArchRule engine_does_not_reach_accessors_or_managers = noClasses()
            .that().resideInAPackage("..svc.engine..")
            .should().dependOnClassesThat().resideInAnyPackage("..svc.accessor..", "..svc.manager..")
            .because("engines are pure business logic with zero dependency on other services");

    @ArchTest
    static final ArchRule engine_is_framework_and_parser_free = noClasses()
            .that().resideInAPackage("..svc.engine..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "freemarker..", "com.fasterxml.jackson..", "org.springframework..")
            .because("the engine compiles to WASM (TeaVM); keep templating, parsers and frameworks off its graph");

    @ArchTest
    static final ArchRule contracts_do_not_depend_on_impls_wiring_or_app = noClasses()
            .that().resideInAPackage("..fw.contract..")
            .should().dependOnClassesThat().resideInAnyPackage("..svc..", "..wiring..", "..app..")
            .because("contracts are the API; they must not depend on impls, wiring or the application");

    @ArchTest
    static final ArchRule services_do_not_depend_upward = noClasses()
            .that().resideInAPackage("..svc..")
            .should().dependOnClassesThat().resideInAnyPackage("..wiring..", "..app..", "org.springframework..")
            .because("services are wired externally; they must not depend on wiring, the app, or a framework");

    @ArchTest
    static final ArchRule no_package_cycles = slices()
            .matching("systems.asymm.cipn.(*)..")
            .should().beFreeOfCycles()
            .because("the layers (fw / svc / wiring / app) form an acyclic, downward dependency graph");
}
