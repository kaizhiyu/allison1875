package com.spldeolin.allison1875.si.processor;

import java.util.Arrays;
import java.util.Collection;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.collection.ast.AstForest;
import com.spldeolin.allison1875.base.collection.vcs.StaticVcsContainer;
import com.spldeolin.allison1875.base.util.ast.Locations;
import com.spldeolin.allison1875.si.dto.LawlessDto;
import com.spldeolin.allison1875.si.dto.PublicAckDto;
import com.spldeolin.allison1875.si.statute.StatuteEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-02-22
 */
@Log4j2
@Accessors(fluent = true)
public class InspectionProcessor {

    @Setter
    private Collection<PublicAckDto> publicAcks;

    @Getter
    private final Collection<LawlessDto> lawlesses = Lists.newLinkedList();

    public InspectionProcessor process() {
        AstForest.getInstance().forEach(cu -> {
            if (StaticVcsContainer.contain(cu)) {
                long start = System.currentTimeMillis();
                Arrays.stream(StatuteEnum.values()).forEach(statuteEnum -> {
                    Collection<LawlessDto> dtos = statuteEnum.getStatute().inspect(cu);
                    dtos.forEach(dto -> {
                        String statuteNo = statuteEnum.getNo();
                        if (isNotInPublicAcks(dto, statuteNo)) {
                            dto.setStatuteNo(statuteNo);
                            lawlesses.add(dto);
                        }
                    });
                });
                log.info("CompilationUnit [{}] inspection completed with [{}]ms.", Locations.getRelativePath(cu),
                        System.currentTimeMillis() - start);
            }
        });

        log.info("All inspections completed");
        return this;
    }

    private boolean isNotInPublicAcks(LawlessDto vo, String statuteNo) {
        String qualifier = vo.getQualifier();
        String sourceCode = vo.getSourceCode();

        for (PublicAckDto pa : publicAcks) {
            if (statuteNo.equals(pa.getStatuteNo())) {
                if (qualifier != null && qualifier.equals(pa.getQualifier())) {
                    return false;
                }
                if (sourceCode.equals(pa.getQualifier())) {
                    return false;
                }
            }
        }
        return true;
    }

}
