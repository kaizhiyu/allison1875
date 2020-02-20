package com.spldeolin.allison1875.da.core.processor;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.classloader.WarOrFatJarClassLoaderFactory;
import com.spldeolin.allison1875.base.constant.QualifierConstants;
import com.spldeolin.allison1875.base.util.Strings;
import com.spldeolin.allison1875.da.core.domain.UriFieldDomain;
import com.spldeolin.allison1875.da.core.enums.FieldTypeEnum;
import com.spldeolin.allison1875.da.core.enums.NumberFormatTypeEnum;
import com.spldeolin.allison1875.da.core.enums.StringFormatTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-01-06
 */
@Log4j2
@Accessors(fluent = true)
class PathVariableProcessor {

    @Setter
    private Collection<Parameter> parameters;

    @Getter
    private final Collection<UriFieldDomain> fields = Lists.newLinkedList();

    private static final JsonSchemaGenerator jsg = new JsonSchemaGenerator(new ObjectMapper());

    PathVariableProcessor process() {
        for (Parameter parameter : parameters) {
            UriFieldDomain field = new UriFieldDomain();
            AnnotationExpr pathVariable = parameter.getAnnotationByName("PathVariable").get();
            String name = null;
            boolean required = false;
            if (pathVariable.isSingleMemberAnnotationExpr()) {
                name = pathVariable.asSingleMemberAnnotationExpr().getMemberValue().asStringLiteralExpr().asString();
            }
            if (pathVariable.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = pathVariable.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    String pairName = pair.getNameAsString();
                    if ("required".equals(pairName)) {
                        required = pair.getValue().asBooleanLiteralExpr().getValue();
                    }
                    if (StringUtils.equalsAny(pairName, "name", "value")) {
                        name = pair.getValue().asStringLiteralExpr().getValue();
                    }
                }
            }
            if (pathVariable.isMarkerAnnotationExpr() || name == null) {
                name = parameter.getNameAsString();
            }
            field.fieldName(name).required(required);

            FieldTypeEnum jsonType;
            NumberFormatTypeEnum numberFormat = null;
            StringBuilder stringFormat = new StringBuilder();
            ResolvedType type = parameter.getType().resolve();
            String describe = type.describe();
            JsonSchema jsonSchema = generateSchema(describe);
            if (jsonSchema != null && jsonSchema.isValueTypeSchema()) {
                if (jsonSchema.isStringSchema()) {
                    jsonType = FieldTypeEnum.string;
                    parameter.getAnnotationByClass(DateTimeFormat.class)
                            .ifPresent(dateTimeFormat -> dateTimeFormat.ifNormalAnnotationExpr(normal -> {
                                normal.getPairs().forEach(pair -> {
                                    if (pair.getNameAsString().equals("pattern")) {
                                        stringFormat
                                                .append(f(StringFormatTypeEnum.datetime.getValue(), pair.getValue()));
                                    }
                                });
                            }));
                    if (stringFormat.length() == 0) {
                        stringFormat.append(StringFormatTypeEnum.normal.getValue());
                    }
                } else if (jsonSchema.isNumberSchema()) {
                    jsonType = FieldTypeEnum.number;

                    if (!jsonSchema.isIntegerSchema()) {
                        numberFormat = NumberFormatTypeEnum.f1oat;
                    } else if (StringUtils.equalsAny(type.describe(), QualifierConstants.INTEGER, "int")) {
                        numberFormat = NumberFormatTypeEnum.int32;
                    } else if (StringUtils.equalsAny(type.describe(), QualifierConstants.LONG, "long")) {
                        numberFormat = NumberFormatTypeEnum.int64;
                    } else {
                        numberFormat = NumberFormatTypeEnum.inT;
                    }
                } else if (jsonSchema.isBooleanSchema()) {
                    jsonType = FieldTypeEnum.bool;
                } else {
                    throw new RuntimeException("impossible unless bug");
                }
            } else {
                log.warn("parameter[{}]不是ValueSchema", parameter);
                continue;
            }
            field.jsonType(jsonType).numberFormat(numberFormat).stringFormat(stringFormat.toString());

            ValidatorProcessor validatorProcessor = new ValidatorProcessor().nodeWithAnnotations(parameter).process();
            field.validators(validatorProcessor.validators());

            fields.add(field);
        }
        return this;
    }

    private JsonSchema generateSchema(String resolvedTypeDescribe) {
        JsonSchema jsonSchema = generateSchemaByQualifierForClassLoader(resolvedTypeDescribe);
        if (jsonSchema == null && resolvedTypeDescribe.contains(".")) {
            generateSchema(Strings.replaceLast(resolvedTypeDescribe, "\\.", "$"));
        }
        return jsonSchema;
    }

    private JsonSchema generateSchemaByQualifierForClassLoader(String qualifierForClassLoader) {
        JavaType javaType;
        try {
            javaType = new TypeFactory(null) {
                private static final long serialVersionUID = -8151903006798193420L;

                @Override
                public ClassLoader getClassLoader() {
                    return WarOrFatJarClassLoaderFactory.getClassLoader();
                }
            }.constructFromCanonical(qualifierForClassLoader);
        } catch (IllegalArgumentException e) {
            log.warn("TypeFactory.constructFromCanonical({})", qualifierForClassLoader);
            return null;
        }
        try {
            return jsg.generateSchema(javaType);
        } catch (JsonMappingException e) {
            log.warn("jsg.generateSchema({})", javaType);
            return null;
        }
    }

}
