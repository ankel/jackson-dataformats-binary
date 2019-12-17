package com.fasterxml.jackson.dataformat.ion.polymorphism;

import com.amazon.ion.IonValue;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * This test checks that {@link IonAnnotationTypeDeserializer} can  deserialize non-annotated input mapping to a
 * generic type using only the generic hint provided with {@link TypeReference}.
 *
 * @author Binh Tran
 */
public class IonAnnotationTypeDeserializerOnNonAnnotatedInputTest {

    private IonValue nonAnnotatedIonValue;

    @Before
    public void setup() throws IOException {
        ClassA inner = new ClassA();
        inner.value = 42;

        ClassB<ClassA> outer = new ClassB<>();
        outer.content = inner;

        IonObjectMapper mapper = new IonObjectMapper();
        nonAnnotatedIonValue = mapper.writeValueAsIonValue(outer);
    }

    @Test
    public void testDeserializeGenericObject() throws IOException {
        final IonObjectMapper mapper = IonObjectMapper.builder()
                .addModule(new IonAnnotationModule())
                .build();

//        IonObjectMapper mapper = new IonObjectMapper();

        ClassB<ClassA> newObj = mapper.readValue(nonAnnotatedIonValue, new TypeReference<ClassB<ClassA>>() {});

        ClassA content = newObj.content;
        assertEquals(42, content.value);
    }

    static class IonAnnotationModule extends SimpleModule {
        private static final long serialVersionUID = 1L;

        IonAnnotationModule() {
            super("IonAnnotationMod", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context) {
            IonAnnotationIntrospector introspector = new ClassNameIonAnnotationIntrospector();
            context.appendAnnotationIntrospector(introspector);
        }
    }

    // For testing, use Jackson's classname TypeIdResolver
    static class ClassNameIonAnnotationIntrospector extends IonAnnotationIntrospector {
        private static final long serialVersionUID = -1L;

        ClassNameIonAnnotationIntrospector() {
            super(true);
        }

        @Override
        protected TypeIdResolver defaultIdResolver(MapperConfig<?> config, JavaType baseType) {
            // use this if you're on tags/jackson-dataformats-binary-2.10.0
            return new MyClassNameIdResolver(baseType, config.getTypeFactory());
            // use this if you're on a newer commit
            // return new ClassNameIdResolver(baseType, ptv);
        }
    }

    private static class MyClassNameIdResolver extends ClassNameIdResolver {
        public MyClassNameIdResolver(JavaType baseType, TypeFactory typeFactory) {
            super(baseType, typeFactory);
        }
    }

    private static class ClassA {
        public int value;
    }

    private static class ClassB<T> {
        public T content;
    }
}
