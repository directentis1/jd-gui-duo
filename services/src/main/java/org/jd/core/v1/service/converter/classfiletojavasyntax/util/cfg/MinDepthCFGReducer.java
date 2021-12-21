package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.apache.bcel.classfile.CodeException;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeUtil;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.*;

public class MinDepthCFGReducer extends ControlFlowGraphReducer {

    @Override
    protected boolean needToUpdateConditionTernaryOperator(BasicBlock basicBlock, BasicBlock nextNext) {
        return ByteCodeUtil.getMinDepth(nextNext) == -1;
    }

    @Override
    protected boolean needToUpdateCondition(BasicBlock basicBlock, BasicBlock nextNext) {
        BasicBlock nextNextNext = nextNext.getNext();
        BasicBlock nextNextBranch = nextNext.getBranch();
        return nextNextNext.getType() == TYPE_GOTO_IN_TERNARY_OPERATOR
                && nextNextNext.getPredecessors().size() == 1
                && nextNextNext.getNext().matchType(TYPE_CONDITIONAL_BRANCH | TYPE_CONDITION)
                && nextNextBranch.matchType(TYPE_STATEMENTS | TYPE_GOTO_IN_TERNARY_OPERATOR)
                && nextNextNext.getNext() == nextNextBranch.getNext()
                && nextNextBranch.getPredecessors().size() == 1
                && nextNextNext.getNext().getPredecessors().size() == 2
                && ByteCodeUtil.getMinDepth(nextNextNext.getNext()) == -2;
    }

    @Override
    protected boolean needToCreateIf(BasicBlock branch, BasicBlock nextNext, int maxOffset) {
        return nextNext.getFromOffset() < branch.getFromOffset() && nextNext.getPredecessors().size() == 1;
    }

    @Override
    protected boolean needToCreateIfElse(BasicBlock branch, BasicBlock nextNext, BasicBlock branchNext) {
        return nextNext.getFromOffset() > branch.getFromOffset() && branchNext.matchType(GROUP_END);
    }

    @Override
    public String makeKey(CodeException ce) {
       return Stream.of(ce.getStartPC(), ce.getEndPC(), ce.getHandlerPC()).map(String::valueOf).collect(Collectors.joining("-"));
    }
    
    @Override
    protected boolean reduceTryDeclaration(BitSet visited, BasicBlock basicBlock, BitSet jsrTargets) {
        BasicBlock next = basicBlock.getNext();
        if (next != null && next.matchType(TYPE_LOOP)) {
            BasicBlock sub1 = next.getSub1();
            if (sub1 != null && sub1.matchType(TYPE_TRY|TYPE_TRY_JSR|TYPE_TRY_ECLIPSE|TYPE_TRY_DECLARATION)) {
                return false;
            }
        }
        return super.reduceTryDeclaration(visited, basicBlock, jsrTargets);
    }
}
