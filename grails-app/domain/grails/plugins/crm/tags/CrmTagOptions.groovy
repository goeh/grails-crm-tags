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
