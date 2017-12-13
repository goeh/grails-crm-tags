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

import groovy.transform.CompileStatic

/**
 * This entity store available options for a tag. For example if the tag is "color"
 * then it could have options like "red", "yellow" and "green".
 * An option can also have a description to be rendered as a help text for the option.
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

    static transients = ['configuration']

    @Override
    String toString() {
        getConfiguration().value.toString()
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

    /**
     * Parse a string with the format "tagname,option1=value,option2=value" into a Map.
     * The tag value is stored with key 'value'.
     *
     * @return a Map of options
     */
    @CompileStatic
    transient Map<String, Object> getConfiguration() {
        Map<String, Object> cfg = parseOption(optionsString)
        cfg.icon = icon
        cfg.text = description
        return cfg
    }

    static Map<String, Object> parseOption(String s) {
        final Map<String, Object> result = [:]
        final StringTokenizer parts = new StringTokenizer(s, ',')

        result.value = parts.nextToken().trim()

        while (parts.hasMoreTokens()) {
            String token = parts.nextToken()
            List<String> tmp = token.split('=').toList()*.trim()
            if (tmp.size() > 1) {
                result[tmp[0]] = removeQuotes(tmp[1])
            } else {
                result.text = token.trim()
                break
            }
        }
        return result
    }

    @CompileStatic
    static private String removeQuotes(String value) {
        if ((value[0] == '\'' && value[-1] == '\'') || (value[0] == '"' && value[-1] == '"')) {
            value = value.substring(1, value.length() - 2)
        }
        return value
    }

    static CrmTagOptions fromString(String s) {
        def map = parseOption(s)
        return new CrmTagOptions(optionsString: s, icon: map.icon, description: map.text)
    }
}
