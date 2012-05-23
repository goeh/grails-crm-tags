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
}
