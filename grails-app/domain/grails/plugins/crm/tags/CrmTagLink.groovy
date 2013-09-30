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

class CrmTagLink {

    CrmTag tag
    String ref
    String value

    static constraints = {
        ref(maxSize: 100, blank: false)
        value(nullable: true)
    }

    static mapping = {
        columns {
            tag index: 'taglink_idx'
            ref index: 'taglink_idx'
        }
        cache usage: "read-write"
    }

    def beforeValidate() {
        if (!tag?.isValidOption(value)) {
            throw new IllegalArgumentException("Value [$value] is not a valid option for tag $tag ${tag?.options}")
        }
    }

    def beforeInsert() {
        if (!tag.multiple) {
            withNewSession {
                int count = CrmTagLink.countByTagAndRef(tag, ref)
                if (count > 0) {
                    throw new IllegalArgumentException("[$tag] does not allow multiple values")
                }
            }
        }
    }

    @Override
    String toString() {
        value.toString()
    }

}
