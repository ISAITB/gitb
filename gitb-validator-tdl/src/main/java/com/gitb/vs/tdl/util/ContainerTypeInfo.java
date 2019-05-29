package com.gitb.vs.tdl.util;

public class ContainerTypeInfo {

    private String containerType;
    private String containedType;

    public ContainerTypeInfo(String containerType, String containedType) {
        this.containerType = containerType;
        this.containedType = containedType;
    }

    public String getContainerType() {
        return containerType;
    }

    public String getContainedType() {
        return containedType;
    }
}
