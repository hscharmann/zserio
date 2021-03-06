package zserio.emit.java;

import java.util.ArrayList;
import java.util.List;

import zserio.ast.ArrayType;
import zserio.ast.BitFieldType;
import zserio.ast.ChoiceType;
import zserio.ast.CompoundType;
import zserio.ast.ZserioType;
import zserio.ast.EnumType;
import zserio.ast.Expression;
import zserio.ast.Field;
import zserio.ast.FixedSizeType;
import zserio.ast.TypeInstantiation;
import zserio.ast.TypeInstantiation.InstantiatedParameter;
import zserio.ast.TypeReference;
import zserio.ast.UnionType;
import zserio.emit.common.ExpressionFormatter;
import zserio.emit.java.types.JavaNativeType;
import zserio.emit.java.types.NativeArrayType;
import zserio.emit.java.types.NativeBooleanType;
import zserio.emit.java.types.NativeDoubleType;
import zserio.emit.java.types.NativeEnumType;
import zserio.emit.java.types.NativeFloatType;
import zserio.emit.java.types.NativeLongType;
import zserio.emit.java.types.NativeObjectArrayType;

public final class CompoundFieldTemplateData
{
    public CompoundFieldTemplateData(JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
            boolean withRangeCheckCode, CompoundType parentType, Field fieldType,
            ExpressionFormatter javaExpressionFormatter)
    {
        name = fieldType.getName();

        // this must be the first one because we need to determine isTypeNullable
        optional = createOptional(fieldType, javaExpressionFormatter);

        final boolean isTypeNullable = (optional != null);
        final ZserioType baseType = TypeReference.resolveBaseType(fieldType.getFieldType());
        final JavaNativeType nativeType = (isTypeNullable) ?
                javaNativeTypeMapper.getNullableJavaType(baseType) : javaNativeTypeMapper.getJavaType(baseType);

        javaTypeName = nativeType.getFullName();
        javaNullableTypeName = javaNativeTypeMapper.getNullableJavaType(baseType).getFullName();

        getterName = AccessorNameFormatter.getGetterName(fieldType);
        setterName = AccessorNameFormatter.getSetterName(fieldType);

        rangeCheckData = new RangeCheckTemplateData(javaNativeTypeMapper, withRangeCheckCode, name, baseType,
                isTypeNullable, javaExpressionFormatter);

        alignmentValue = createAlignmentValue(fieldType, javaExpressionFormatter);
        initializer = createInitializer(fieldType, javaExpressionFormatter);

        usesObjectChoice = (parentType instanceof ChoiceType) || (parentType instanceof UnionType);

        isBool = nativeType instanceof NativeBooleanType;
        isLong = nativeType instanceof NativeLongType;
        isFloat = nativeType instanceof NativeFloatType;
        isDouble = nativeType instanceof NativeDoubleType;
        isEnum = nativeType instanceof NativeEnumType;
        isSimpleType = nativeType.isSimple();
        isObjectArray = nativeType instanceof NativeObjectArrayType;

        constraint = createConstraint(fieldType, javaExpressionFormatter);

        bitSize = new BitSize(baseType, javaNativeTypeMapper, javaExpressionFormatter);
        offset = createOffset(fieldType, javaNativeTypeMapper, javaExpressionFormatter);
        array = createArray(nativeType, baseType, parentType, javaNativeTypeMapper, withWriterCode,
                javaExpressionFormatter);
        runtimeFunction = JavaRuntimeFunctionDataCreator.createData(baseType, javaExpressionFormatter,
                javaNativeTypeMapper);
        compound = createCompound(javaNativeTypeMapper, withWriterCode, javaExpressionFormatter, parentType,
                baseType);
    }

    public String getName()
    {
        return name;
    }

    public String getJavaTypeName()
    {
        return javaTypeName;
    }

    public String getJavaNullableTypeName()
    {
        return javaNullableTypeName;
    }

    public String getGetterName()
    {
        return getterName;
    }

    public String getSetterName()
    {
        return setterName;
    }

    public RangeCheckTemplateData getRangeCheckData()
    {
        return rangeCheckData;
    }

    public Optional getOptional()
    {
        return optional;
    }

    public String getAlignmentValue()
    {
        return alignmentValue;
    }

    public String getInitializer()
    {
        return initializer;
    }

    public boolean getUsesObjectChoice()
    {
        return usesObjectChoice;
    }

    public boolean getIsBool()
    {
        return isBool;
    }

    public boolean getIsLong()
    {
        return isLong;
    }

    public boolean getIsFloat()
    {
        return isFloat;
    }

    public boolean getIsDouble()
    {
        return isDouble;
    }

    public boolean getIsEnum()
    {
        return isEnum;
    }

    public boolean getIsSimpleType()
    {
        return isSimpleType;
    }

    public boolean getIsObjectArray()
    {
        return isObjectArray;
    }

    public String getConstraint()
    {
        return constraint;
    }

    public BitSize getBitSize()
    {
        return bitSize;
    }

    public Offset getOffset()
    {
        return offset;
    }

    public Array getArray()
    {
        return array;
    }

    public RuntimeFunctionTemplateData getRuntimeFunction()
    {
        return runtimeFunction;
    }

    public Compound getCompound()
    {
        return compound;
    }

    public static class Optional
    {
        public Optional(Expression optionalClauseExpression, String indicatorName,
                ExpressionFormatter javaExpressionFormatter)
        {
            clause = (optionalClauseExpression == null) ? null :
                javaExpressionFormatter.formatGetter(optionalClauseExpression);
            this.indicatorName = indicatorName;
        }

        public String getClause()
        {
            return clause;
        }

        public String getIndicatorName()
        {
            return indicatorName;
        }

        private final String  clause;
        private final String  indicatorName;
    }

    public static class BitSize
    {
        public BitSize(ZserioType type, JavaNativeTypeMapper javaNativeTypeMapper,
                ExpressionFormatter javaExpressionFormatter)
        {
            value = createValue(type, javaExpressionFormatter);
            runtimeFunction = (value != null) ? null :
                JavaRuntimeFunctionDataCreator.createData(type, javaExpressionFormatter, javaNativeTypeMapper);
        }

        public String getValue()
        {
            return value;
        }

        public RuntimeFunctionTemplateData getRuntimeFunction()
        {
            return runtimeFunction;
        }

        private static String createValue(ZserioType type, ExpressionFormatter javaExpressionFormatter)
        {
            String bitSizeOfValue = null;
            if (type instanceof FixedSizeType)
            {
                bitSizeOfValue = JavaLiteralFormatter.formatIntegerLiteral(((FixedSizeType)type).getBitSize());
            }
            else if (type instanceof BitFieldType)
            {
                final BitFieldType bitFieldType = (BitFieldType)type;
                final Integer bitSize = bitFieldType.getBitSize();
                bitSizeOfValue = (bitSize != null) ? JavaLiteralFormatter.formatIntegerLiteral(bitSize) :
                    javaExpressionFormatter.formatGetter(bitFieldType.getLengthExpression());
            }

            return bitSizeOfValue;
        }

        private final String                        value;
        private final RuntimeFunctionTemplateData   runtimeFunction;
    }

    public static class Offset
    {
        public Offset(Expression offsetExpression, JavaNativeTypeMapper javaNativeTypeMapper,
                     ExpressionFormatter javaExpressionFormatter)
        {
            getter = javaExpressionFormatter.formatGetter(offsetExpression);
            setter = javaExpressionFormatter.formatSetter(offsetExpression);
            typeName = javaNativeTypeMapper.getJavaType(offsetExpression.getExprZserioType()).getFullName();
            containsIndex = offsetExpression.containsIndex();
        }

        public String getGetter()
        {
            return getter;
        }

        public String getSetter()
        {
            return setter;
        }

        public String getTypeName()
        {
            return typeName;
        }

        public boolean getContainsIndex()
        {
            return containsIndex;
        }

        private final String    getter;
        private final String    setter;
        private final String    typeName;
        private final boolean   containsIndex;
    }

    public static class Array
    {
        public Array(NativeArrayType nativeType, ArrayType baseType, CompoundType parentType,
                JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
                ExpressionFormatter javaExpressionFormatter)
        {
            isImplicit = baseType.isImplicit();
            length = createLength(baseType, javaExpressionFormatter);
            final ZserioType elementType = TypeReference.resolveBaseType(baseType.getElementType());
            final JavaNativeType elementNativeType = javaNativeTypeMapper.getJavaType(elementType);
            elementJavaTypeName = elementNativeType.getFullName();

            requiresElementBitSize = nativeType.requiresElementBitSize();
            requiresElementFactory = nativeType.requiresElementFactory();
            requiresParentContext = createRequiresParentContext(baseType);

            generateListSetter = createGenerateListSetter(elementType);

            elementBitSize = new BitSize(elementType, javaNativeTypeMapper, javaExpressionFormatter);
            isElementEnum = elementNativeType instanceof NativeEnumType;
            elementCompound = createCompound(javaNativeTypeMapper, withWriterCode, javaExpressionFormatter,
                    parentType, elementType);
        }

        public boolean getIsImplicit()
        {
            return isImplicit;
        }

        public String getLength()
        {
            return length;
        }

        public String getElementJavaTypeName()
        {
            return elementJavaTypeName;
        }

        public boolean getRequiresElementBitSize()
        {
            return requiresElementBitSize;
        }

        public boolean getRequiresElementFactory()
        {
            return requiresElementFactory;
        }

        public boolean getRequiresParentContext()
        {
            return requiresParentContext;
        }

        public boolean getGenerateListSetter()
        {
            return generateListSetter;
        }

        public BitSize getElementBitSize()
        {
            return elementBitSize;
        }

        public boolean getIsElementEnum()
        {
            return isElementEnum;
        }

        public Compound getElementCompound()
        {
            return elementCompound;
        }

        private static String createLength(ArrayType arrayType, ExpressionFormatter javaExpressionFormatter)
        {
            final Expression lengthExpression = arrayType.getLengthExpression();
            if (lengthExpression == null)
                return null;

            return javaExpressionFormatter.formatGetter(lengthExpression);
        }

        private static boolean createRequiresParentContext(ArrayType arrayType)
        {
            /*
             * Array length expression (Foo field[expr];) is not needed here because it's handled by the array
             * class itself, it's not propagated to the array factory.
             * But an array can be composed of type instantiations and these need to be handled.
             */
            ZserioType baseType = TypeReference.resolveBaseType(arrayType.getElementType());

            if (baseType instanceof TypeInstantiation)
            {
                final TypeInstantiation typeInstantiation = (TypeInstantiation)baseType;
                for (TypeInstantiation.InstantiatedParameter instantiatedParameter :
                        typeInstantiation.getInstantiatedParameters())
                {
                    if (instantiatedParameter.getArgumentExpression().requiresOwnerContext())
                        return true;
                }
            }

            return false;
        }

        private static boolean createGenerateListSetter(ZserioType elementType)
        {
            return elementType instanceof CompoundType || elementType instanceof EnumType ||
                elementType instanceof TypeInstantiation;
        }

        private final boolean       isImplicit;
        private final String        length;
        private final String        elementJavaTypeName;
        private final boolean       requiresElementBitSize;
        private final boolean       requiresElementFactory;
        private final boolean       requiresParentContext;
        private final boolean       generateListSetter;
        private final BitSize       elementBitSize;
        private final boolean       isElementEnum;
        private final Compound      elementCompound;
    }

    public static class Compound
    {
        public Compound(JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
                CompoundType owner, CompoundType compoundFieldType)
        {
            instantiatedParameters = new ArrayList<InstantiatedParameterData>(0);
        }

        public Compound(JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
                ExpressionFormatter javaExpressionFormatter, CompoundType owner,
                TypeInstantiation compoundFieldType)
        {
            final List<InstantiatedParameter> parameters = compoundFieldType.getInstantiatedParameters();
            instantiatedParameters = new ArrayList<InstantiatedParameterData>(parameters.size());
            for (InstantiatedParameter parameter : parameters)
                instantiatedParameters.add(new InstantiatedParameterData(javaNativeTypeMapper,
                        javaExpressionFormatter, parameter));
        }

        public Iterable<InstantiatedParameterData> getInstantiatedParameters()
        {
            return instantiatedParameters;
        }

        public static class InstantiatedParameterData
        {
            public InstantiatedParameterData(JavaNativeTypeMapper javaNativeTypeMapper,
                    ExpressionFormatter javaExpressionFormatter, InstantiatedParameter instantiatedParameter)
            {
                final ZserioType parameterType = instantiatedParameter.getParameter().getParameterType();
                final JavaNativeType nativeParameterType = javaNativeTypeMapper.getJavaType(parameterType);
                javaTypeName = nativeParameterType.getFullName();
                isSimpleType = nativeParameterType.isSimple();
                expression = javaExpressionFormatter.formatGetter(
                        instantiatedParameter.getArgumentExpression());
            }

            public String getJavaTypeName()
            {
                return javaTypeName;
            }

            public boolean getIsSimpleType()
            {
                return isSimpleType;
            }

            public String getExpression()
            {
                return expression;
            }

            private final String    javaTypeName;
            private final boolean   isSimpleType;
            private final String    expression;
        }

        final ArrayList<InstantiatedParameterData>  instantiatedParameters;
    }

    private static Optional createOptional(Field fieldType, ExpressionFormatter javaExpressionFormatter)
    {
        if (!fieldType.getIsOptional())
            return null;

        final Expression optionalClauseExpression = fieldType.getOptionalClauseExpr();
        final String indicatorName = AccessorNameFormatter.getIndicatorName(fieldType);

        return new Optional(optionalClauseExpression, indicatorName, javaExpressionFormatter);
    }

    private static String createInitializer(Field fieldType, ExpressionFormatter javaExpressionFormatter)
    {
        final Expression initializerExpression = fieldType.getInitializerExpr();
        if (initializerExpression == null)
            return null;

        return javaExpressionFormatter.formatGetter(initializerExpression);
    }

    private static String createAlignmentValue(Field fieldType, ExpressionFormatter javaExpressionFormatter)
    {
        final Expression alignmentExpression = fieldType.getAlignmentExpr();
        if (alignmentExpression == null)
            return null;

        return javaExpressionFormatter.formatGetter(alignmentExpression);
    }

    private static String createConstraint(Field fieldType, ExpressionFormatter javaExpressionFormatter)
    {
        final Expression constraintExpression = fieldType.getConstraintExpr();
        if (constraintExpression == null)
            return null;

        return javaExpressionFormatter.formatGetter(constraintExpression);
    }

    private static Offset createOffset(Field field, JavaNativeTypeMapper javaNativeTypeMapper,
            ExpressionFormatter javaExpressionFormatter)
    {
        final Expression offsetExpression = field.getOffsetExpr();
        if (offsetExpression == null)
            return null;

        return new Offset(offsetExpression, javaNativeTypeMapper, javaExpressionFormatter);
    }

    private static Array createArray(JavaNativeType nativeType, ZserioType baseType, CompoundType parentType,
            JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
            ExpressionFormatter javaExpressionFormatter)
    {
        if (!(nativeType instanceof NativeArrayType))
            return null;

        if (!(baseType instanceof ArrayType))
            throw new RuntimeException("inconsistent base type and native type");

        return new Array((NativeArrayType)nativeType, (ArrayType)baseType, parentType, javaNativeTypeMapper,
                withWriterCode, javaExpressionFormatter);
    }

    private static Compound createCompound(JavaNativeTypeMapper javaNativeTypeMapper, boolean withWriterCode,
            ExpressionFormatter javaExpressionFormatter, CompoundType owner, ZserioType baseType)
    {
        if (baseType instanceof CompoundType)
            return new Compound(javaNativeTypeMapper, withWriterCode, owner, (CompoundType)baseType);
        else if (baseType instanceof TypeInstantiation)
            return new Compound(javaNativeTypeMapper, withWriterCode, javaExpressionFormatter, owner,
                    (TypeInstantiation)baseType);
        else
            return null;
    }

    private final String                        name;
    private final String                        javaTypeName;
    private final String                        javaNullableTypeName;
    private final String                        getterName;
    private final String                        setterName;
    private final RangeCheckTemplateData        rangeCheckData;
    private final Optional                      optional;
    private final String                        alignmentValue;
    private final String                        initializer;

    private final boolean                       usesObjectChoice;

    private final boolean                       isBool;
    private final boolean                       isLong;
    private final boolean                       isFloat;
    private final boolean                       isDouble;
    private final boolean                       isEnum;
    private final boolean                       isSimpleType;
    private final boolean                       isObjectArray;
    private final String                        constraint;

    private final BitSize                       bitSize;
    private final Offset                        offset;
    private final Array                         array;
    private final RuntimeFunctionTemplateData   runtimeFunction;
    private final Compound                      compound;
}
