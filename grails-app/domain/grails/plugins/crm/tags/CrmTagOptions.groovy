/*
 * Copyright (c) 2014 Goran Ehrsson.
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
 */

package grails.plugins.crm.tags

/**
 * Created by goran on 2014-07-03.
 */
class CrmTagOptions implements Comparable<CrmTagOptions> {

    String optionsString
    String icon
    String description

    static constraints = {
        optionsString(maxSize: 255)
        icon(maxSize: 255, nullable: true)
        description(maxSize: 2000, nullable: true, widget: 'textarea')
    }

    static belongsTo = [crmTag: CrmTag]

    CrmTagOptions() {
    }

    CrmTagOptions(String arg) {
        optionsString = arg
    }

    @Override
    String toString() {
        optionsString.toString()
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        CrmTagOptions that = (CrmTagOptions) o

        if (crmTag != that.crmTag) return false
        if (optionsString != that.optionsString) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (optionsString != null ? optionsString.hashCode() : 0)
        result = 31 * result + (crmTag != null ? crmTag.hashCode() : 0)
        return result
    }

    @Override
    int compareTo(CrmTagOptions o) {
        optionsString.compareTo(o.optionsString)
    }
}
