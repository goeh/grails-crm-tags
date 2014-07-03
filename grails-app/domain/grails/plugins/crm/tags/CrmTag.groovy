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

import grails.plugins.crm.core.CrmLookupEntity
import grails.util.GrailsNameUtils

class CrmTag extends CrmLookupEntity {
    CrmTag parent
    boolean mustMatch
    boolean multiple
    static hasMany = [children: CrmTag, options: CrmTagOptions]
    static mappedBy = [children: 'parent']
    static constraints = {
        parent(nullable: true)
    }
    static transients = ['propertyName', 'usage']

    boolean isValidOption(value) {
        if (mustMatch) {
            if(options) {
                return (options*.optionsString).contains(value.toString())
            }
            return false
        }
        return true
    }

    transient String getPropertyName() {
        GrailsNameUtils.getPropertyName(name)
    }

    transient int getUsage(Object value = null) {
        CrmTagLink.createCriteria().count {
            eq('tag', this)
            if (value) {
                eq('value', value.toString())
            }
            cache true
        }
    }

    CrmTag addToOptions(String opt) {
        addToOptions(new CrmTagOptions(opt))
    }

    void removeFromOptions(String opt) {
        CrmTagOptions existing = options?.find { it.optionsString == opt }
        if (existing) {
            removeFromOptions(existing)
        }
    }
}
