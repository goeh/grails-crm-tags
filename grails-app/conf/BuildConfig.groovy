grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.repos.default = "crm"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        //mavenCentral()
        mavenRepo "http://labs.technipelago.se/repo/plugins-releases-local/"
        mavenRepo "http://labs.technipelago.se/repo/crm-releases-local/"
    }
    dependencies {
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:2.0.4") {
            export = false
        }
        runtime(":hibernate:$grailsVersion") {
            export = false
        }
        test(":spock:0.6") { export = false }
        test(":codenarc:latest.integration") { export = false }

        compile("grails.crm:crm-core:latest.integration")

        runtime ":selection:latest.integration"
    }
}

codenarc {
    reports = {
        CrmXmlReport('xml') {
            outputFile = 'CodeNarcReport.xml'
            title = 'Grails CRM CodeNarc Report'
        }
        CrmHtmlReport('html') {
            outputFile = 'target/test-reports/CodeNarcReport.html'
            title = 'Grails CRM CodeNarc Report'
        }
    }
    processTestUnit = false
    processTestIntegration = false
}
