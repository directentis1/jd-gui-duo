/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.Map;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.accessor.Accessor;
import jd.core.model.classfile.accessor.AccessorConstants;
import jd.core.model.classfile.accessor.GetFieldAccessor;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;

/*
 * Replace 'TestInnerClass.access$0(this.this$0)'
 * par 'this.this$0.text'
 */
public class OuterGetFieldVisitor extends OuterGetStaticVisitor
{
    public OuterGetFieldVisitor(
        Map<String, ClassFile> innerClassesMap, ConstantPool constants)
    {
        super(innerClassesMap, constants);
    }

    @Override
    protected Accessor match(Instruction i)
    {
        if (i.getOpcode() != Const.INVOKESTATIC)
            return null;

        Invokestatic is = (Invokestatic)i;
        ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
        ConstantNameAndType cnat =
            constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String descriptor =
            constants.getConstantUtf8(cnat.getSignatureIndex());

        // Two parameters ?
        if (cmr.getNbrOfParameters() != 1)
            return null;

        String className = constants.getConstantClassName(cmr.getClassIndex());
        ClassFile classFile = this.innerClassesMap.get(className);
        if (classFile == null)
            return null;

        String name =
            constants.getConstantUtf8(cnat.getNameIndex());

        Accessor accessor = classFile.getAccessor(name, descriptor);

        if ((accessor == null) ||
            (accessor.getTag() != AccessorConstants.ACCESSOR_GETFIELD))
            return null;

        return accessor;
    }

    @Override
    protected Instruction newInstruction(Instruction i, Accessor a)
    {
        GetFieldAccessor gfa = (GetFieldAccessor)a;
        Invokestatic is = (Invokestatic)i;

        int nameIndex = this.constants.addConstantUtf8(gfa.getFieldName());
        int descriptorIndex =
            this.constants.addConstantUtf8(gfa.getFieldDescriptor());
        int cnatIndex =
            this.constants.addConstantNameAndType(nameIndex, descriptorIndex);

        int classNameIndex = this.constants.addConstantUtf8(gfa.getClassName());
        int classIndex = this.constants.addConstantClass(classNameIndex);

        int cfrIndex =
             this.constants.addConstantFieldref(classIndex, cnatIndex);

        Instruction objectref = is.getArgs().remove(0);

        return new GetField(
            Const.GETFIELD, i.getOffset(), i.getLineNumber(),
            cfrIndex, objectref);
    }
}