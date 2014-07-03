/*
 * Copyright (c) 2012 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */

package grails.plugins.crm.tags

import grails.plugins.crm.core.CrmException

class CrmTagServiceSpec extends grails.plugin.spock.IntegrationSpec {

    def crmTagService

    def "set tag value"() {
        given:
        crmTagService.createTag(name: "Foo")
        new TestEntity(name: "A").save(failOnError: true)
        new TestEntity(name: "B").save(failOnError: true)
        new TestEntity(name: "C").save(failOnError: true)

        when:
        def testEntity = TestEntity.findByName("B")
        then:
        testEntity != null

        when:
        testEntity.setTagValue("Foo", 42)
        then:
        testEntity.getTagValue("Foo") == "42"
        CrmTagLink.list().iterator().next().value == "42"

        TestEntity.findAllByTag("Foo", 42).size() == 1
        TestEntity.findAllByTag("Foo", 24).size() == 0
    }

    def "delete tag value"() {
        given:
        crmTagService.createTag(name: "Foo")
        new TestEntity(name: "A").save(failOnError: true)
        new TestEntity(name: "B").save(failOnError: true)
        new TestEntity(name: "C").save(failOnError: true)

        when:
        def testEntity = TestEntity.findByName("B")
        then:
        testEntity != null

        when:
        testEntity.setTagValue("Foo", 42)
        then:
        testEntity.getTagValue("Foo") == "42"
        CrmTagLink.list().iterator().next().value == "42"

        TestEntity.findAllByTag("Foo", 42).size() == 1
        TestEntity.findAllByTag("Foo", 24).size() == 0

        when:
        testEntity.deleteTag("Foo")
        then:
        testEntity.getTagValue("Foo") == null
        TestEntity.findAllByTag("Foo", 42).size() == 0
    }

    def "options are sorted"() {
        when:
        def tag = crmTagService.createTag(name: "SortedOptions", options: ['bronce', 'silver', 'gold', 'platina', 'aluminium'])

        then:
        tag.options.sort()*.toString() == ['aluminium', 'bronce', 'gold', 'platina', 'silver']
    }

    def "valid tag options"() {
        given:
        crmTagService.createTag(name: "Score", description: "Score", mustMatch: true, options: ['1', '2', '3', '4', '5'])
        new TestEntity(name: "A").save(failOnError: true)

        when:
        def testEntity = TestEntity.findByName("A")
        then:
        testEntity != null

        when:
        testEntity.setTagValue("Score", score)
        then:
        testEntity.getTagValue("Score") == expected
        where:
        score | expected
        1     | "1"
        2     | "2"
        3     | "3"
        4     | "4"
        5     | "5"
    }

    def "parse tag option"() {
        when:
        def (opt, cfg) = crmTagService.parseTagOption("foo[icon=open,width=640, height=320]")

        then:
        opt == 'foo'
        cfg.icon == 'open'
        cfg.width == '640'
        cfg.height == '320'

        when:
        (opt, cfg) = crmTagService.parseTagOption(" foo [icon=open ,width = 640, height=320 ] ")

        then:
        opt == 'foo'
        cfg.icon == 'open'
        cfg.width == '640'
        cfg.height == '320'

        when:
        (opt, cfg) = crmTagService.parseTagOption("foo[ hello ]")

        then:
        opt == 'foo'
        cfg.text == 'hello'
    }

    def "create tag with complex options"() {
        when:
        def tag = crmTagService.createTag(name: "Complex", options: ['bronce', 'silver', 'gold[icon=winner]'])

        then:
        tag.options.find{it.icon == 'winner'}.toString() == 'gold'
        tag.options.find{it.toString() == 'silver'}.icon == null
    }

    def "invalid tag options"() {
        given:
        crmTagService.createTag(name: "Score", description: "Score", mustMatch: true, options: ['1', '2', '3', '4', '5'])
        new TestEntity(name: "A").save(failOnError: true)

        when:
        def testEntity = TestEntity.findByName("A")
        then:
        testEntity != null

        when:
        testEntity.setTagValue("Score", 0)
        then:
        thrown(CrmException)

        when:
        testEntity.setTagValue("Score", 6)
        then:
        thrown(CrmException)
    }

    def "get class tags"() {
        given:
        crmTagService.createTag(name: TestEntity.name, multiple: true)
        def m = new TestEntity(name: "A").save(failOnError: true)

        expect:
        m.getClassTags().isEmpty()

        when:
        m.setTagValue("foo")

        then:
        m.getClassTags().size() == 1

        when:
        m.setTagValue("bar")
        m.setTagValue("baz")

        then:
        m.classTags.size() == 3

        when:
        m.deleteTagValue("baz")

        then:
        m.classTags.size() == 2
        m.classTags.sort() == ["bar", "foo"]
    }

    def "test setTagValue chaining"() {
        given:
        crmTagService.createTag(name: TestEntity.name, multiple: true)
        def m = new TestEntity(name: "C").save(failOnError: true)

        expect:
        m.getClassTags().isEmpty()

        when:
        m.setTagValue("foo").setTagValue("bar")

        then:
        m.getClassTags().size() == 2
        m.isTagged("foo")
        m.isTagged("bar")

        when:
        m.deleteTagValue("foo").setTagValue("42")

        then:
        m.getClassTags().size() == 2
        !m.isTagged("foo")
        m.isTagged("bar")
        m.isTagged("42")
    }

    def "list distinct values"() {
        when:
        crmTagService.createTag(name: TestEntity.name, multiple: true)
        new TestEntity(name: "A").save(failOnError: true).setTagValue("blue")
        new TestEntity(name: "B").save(failOnError: true).setTagValue("red").setTagValue("green")
        new TestEntity(name: "C").save(failOnError: true).setTagValue("blue").setTagValue("green").setTagValue("gray")

        then:
        crmTagService.listDistinctValue(TestEntity.name).size() == 4
        crmTagService.listDistinctValue(TestEntity.name, 'b').size() == 1
        crmTagService.listDistinctValue(TestEntity.name, 'g').size() == 2
    }

    def "find primary keys"() {
        when:
        crmTagService.createTag(name: TestEntity.name, multiple: true)
        new TestEntity(name: "A").save(failOnError: true).setTagValue("blue")
        new TestEntity(name: "B").save(failOnError: true).setTagValue("red").setTagValue("green")
        new TestEntity(name: "C").save(failOnError: true).setTagValue("blue").setTagValue("green").setTagValue("gray")
        new TestEntity(name: "D").save(failOnError: true).setTagValue("yellow")
        def result = TestEntity.list()*.id.sort()

        then:
        crmTagService.findAllIdByTag(TestEntity, 'green').size() == 2
        crmTagService.findAllIdByTag(TestEntity, 'green').find { it == result[1] }
        crmTagService.findAllIdByTag(TestEntity, 'green').find { it == result[2] }
        crmTagService.findAllIdByTag(TestEntity, 'blue,green').size() == 3
        crmTagService.findAllIdByTag(TestEntity, 'blue&green').size() == 1
        crmTagService.findAllIdByTag(TestEntity).size() == 4
    }

    def "tags can have child tags"() {
        given:
        def tag = crmTagService.createTag(name: TestEntity.name, multiple: true)
        crmTagService.createTag(parent: tag, name: "make", multiple: true)
        crmTagService.createTag(parent: tag, name: "model", multiple: true)

        when:
        def car = new TestEntity(name: "I'm selling my old volvo")
                .save(failOnError: true)
                .setTagValue("make", "volvo")
                .setTagValue("model", "xc90")

        then:
        car.isTagged('make', 'volvo')
        car.isTagged('model', 'xc90')

        !car.isTagged('model', 'volvo')
        !car.isTagged('make', 'xc90')

        when:
        car.deleteTag('model')

        then:
        car.isTagged('make', 'volvo')
        !car.isTagged('model', 'xc90')
    }
}
