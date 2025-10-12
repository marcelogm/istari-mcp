package com.marcelogm.istarimcp.domain.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public enum RelationshipType {
    HAS("HAS"),
    IS_A("IS_A"),
    PART_OF("PART_OF"),
    BELONGS_TO("BELONGS_TO"),
    CONTAINS("CONTAINS"),
    KNOWS("KNOWS"),
    USES("USES"),
    CREATES("CREATES"),
    MODIFIES("MODIFIES"),
    READS("READS"),
    OWNS("OWNS"),
    ATTENDS("ATTENDS"),
    CAUSES("CAUSES"),
    INFLUENCES("INFLUENCES"),
    REQUIRES("REQUIRES"),
    DEPENDS_ON("DEPENDS_ON"),
    LEADS_TO("LEADS_TO"),
    PRECEDES("PRECEDES"),
    FOLLOWS("FOLLOWS"),
    OCCURRED_DURING("OCCURRED_DURING"),
    SIMILAR_TO("SIMILAR_TO"),
    RELATED_TO("RELATED_TO"),
    HAS_OBSERVATION("HAS_OBSERVATION"),
    INVOKES("INVOKES"),
    SENDS("SENDS"),
    RECEIVES("RECEIVES"),
    COUPLED_WITH("COUPLED_WITH"),
    IMPLEMENTS("IMPLEMENTS"),
    EXTENDS("EXTENDS"),
    CONFIGURES("CONFIGURES"),
    PROVIDES("PROVIDES"),
    CONSUMES("CONSUMES"),
    GROUPS("GROUPS"),
    COHESIVE_WITH("COHESIVE_WITH"),
    RESIDES_IN("RESIDES_IN"),
    CONNECTS("CONNECTS"),
    ACCESSES("ACCESSES"),
    EXECUTES("EXECUTES"),
    MANAGES("MANAGES"),
    TRIGGERS("TRIGGERS"),
    MONITORS("MONITORS"),
    SECURES("SECURES"),
    BACKUP("BACKUP"),
    RESTORE("RESTORE"),
    LOGS("LOGS"),
    REPORTS("REPORTS"),
    ALARMS("ALARMS"),
    NOTIFIES("NOTIFIES");

    private final String type;

    RelationshipType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public static RelationshipType fromString(String typeString) {
        for (RelationshipType type : RelationshipType.values()) {
            if (type.type.equalsIgnoreCase(typeString)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for type string: " + typeString);
    }
}