package com.spldeolin.allison1875.da.approved.javabean;

import java.util.Collection;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 一个javabean内所有property（包括所有嵌套的property）的容器
 *
 * @author Deolin 2020-05-08
 */
@Data
@Accessors(chain = true)
public class JavabeanPropertyContainer {

    private String javabeanQualifier;

    /**
     * 树状的JavabeanProperty
     */
    private Collection<JavabeanProperty> dendriformProperties;

    /**
     * 平铺的JavabeanProperty
     */
    private Collection<JavabeanProperty> flatProperties;

    public JavabeanPropertyContainer(String javabeanQualifier, Collection<JavabeanProperty> dendriformProperties) {
        this.javabeanQualifier = javabeanQualifier;
        this.dendriformProperties = dendriformProperties;
        flat();
        buildAllPath();
    }

    private void flat() {
        flatProperties = Lists.newLinkedList();
        if (dendriformProperties != null) {
            for (JavabeanProperty firstFloor : dendriformProperties) {
                flat(firstFloor, flatProperties);
            }
        }
    }

    private void buildAllPath() {
        if (flatProperties == null) {
            flat();
        }
        for (JavabeanProperty prop : flatProperties) {
            String name = prop.getName();
            StringBuilder path = new StringBuilder(name);
            this.insertToHead(prop, path);
            prop.setPath(path.toString());
        }
    }

    private void insertToHead(JavabeanProperty prop, StringBuilder path) {
        JavabeanProperty parent = prop.getParent();
        if (parent != null) {
            String linkPart = parent.getName();
            if (parent.getJsonType().isArrayLike()) {
                linkPart += "[0]";
            }
            path.insert(0, linkPart + ".");
            this.insertToHead(parent, path);
        }
    }

    private void flat(JavabeanProperty parent, Collection<JavabeanProperty> props) {
        if (parent.getChildren() != null) {
            for (JavabeanProperty child : parent.getChildren()) {
                props.add(child);
                this.flat(child, props);
            }
        }
    }

}