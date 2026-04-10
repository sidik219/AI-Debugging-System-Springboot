package com.llm.ai.core.common;

import com.llm.ai.project.debuggingAI.model.ErrorContext;
import com.llm.ai.project.debuggingAI.service.GroqService;
import com.llm.ai.project.debuggingAI.util.ConsoleColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Service
public class UnitTestGenerator {

    @Autowired
    private GroqService groqService;

    private enum ClassType {
        CONTROLLER, SERVICE, MODEL, REPOSITORY, UNKNOWN
    }

    // TODO: ==================== PUBLIC API ====================

    public String generateUnitTest(ErrorContext context) {
        String className = context.getClassName();
        return generateAllUnitTests(className);
    }

    public String generateAllUnitTests(String className) {
        System.out.println(ConsoleColors.CYAN + "🧪 Generating unit tests untuk: " + className + ConsoleColors.RESET);

        try {
            Class<?> clazz = Class.forName(className);
            String packageName = clazz.getPackage().getName();
            String simpleClassName = clazz.getSimpleName();
            ClassType classType = detectClassType(clazz);

            System.out.println(ConsoleColors.CYAN + "   📦 Class type: " + classType + ConsoleColors.RESET);

            String testCode = generateTestByType(clazz, packageName, simpleClassName, classType);
            saveTestFile(packageName, simpleClassName, testCode);

            return "✅ Unit test generated untuk " + simpleClassName;

        } catch (ClassNotFoundException e) {
            return "❌ Class tidak ditemukan: " + className;
        }
    }

    // TODO: ==================== CLASS TYPE DETECTION ====================

    private ClassType detectClassType(Class<?> clazz) {
        if (clazz.isAnnotationPresent(RestController.class) ||
                clazz.isAnnotationPresent(Controller.class)) {
            return ClassType.CONTROLLER;
        }
        if (clazz.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            return ClassType.SERVICE;
        }
        if (clazz.isAnnotationPresent(org.springframework.stereotype.Repository.class)) {
            return ClassType.REPOSITORY;
        }
        if (clazz.isAnnotationPresent(lombok.Data.class) ||
                clazz.isAnnotationPresent(lombok.Getter.class) ||
                hasGetterSetter(clazz)) {
            return ClassType.MODEL;
        }
        return ClassType.UNKNOWN;
    }

    private boolean hasGetterSetter(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().startsWith("get") || m.getName().startsWith("set")) {
                return true;
            }
        }
        return false;
    }

    // TODO: ==================== GENERATE BY TYPE ====================

    private String generateTestByType(Class<?> clazz, String packageName, String className, ClassType classType) {
        return switch (classType) {
            case CONTROLLER -> generateControllerTest(clazz, packageName, className);
            case SERVICE -> generateServiceTest(clazz, packageName, className);
            case REPOSITORY -> generateRepositoryTest(clazz, packageName, className);
            case MODEL -> generateModelTest(clazz, packageName, className);
            default -> generateGenericTest(packageName, className);
        };
    }

    // TODO: ==================== CONTROLLER TEST (Akurasi 95%) ====================

    private String generateControllerTest(Class<?> clazz, String packageName, String className) {
        String basePath = extractBasePath(clazz);
        List<EndpointInfo> endpoints = extractEndpoints(clazz, basePath);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            package %s;
            
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.DisplayName;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
            import org.springframework.test.web.servlet.MockMvc;
            
            import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
            import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
            
            @WebMvcTest(%s.class)
            class %sTest {
                
                @Autowired
                private MockMvc mockMvc;
            
            """, packageName, className, className));

        for (EndpointInfo ep : endpoints) {
            String methodName = "test" + capitalize(ep.methodName);
            boolean isErrorEndpoint = ep.path.contains("error") ||
                    ep.path.contains("divide") ||
                    ep.path.contains("array");

            sb.append(String.format("""
                
                @Test
                @DisplayName("Test %s %s")
                void %s() throws Exception {
                    // Arrange
                    String url = "%s";
                    
                    // Act & Assert
                    mockMvc.perform(get(url))
                            .andExpect(status().%s);
                }
                """,
                    ep.httpMethod, ep.path, methodName, ep.path,
                    isErrorEndpoint ? "is5xxServerError()" : "isOk()"
            ));
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String extractBasePath(Class<?> clazz) {
        RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
        return (rm != null && rm.value().length > 0) ? rm.value()[0] : "";
    }

    private List<EndpointInfo> extractEndpoints(Class<?> clazz, String basePath) {
        List<EndpointInfo> endpoints = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            GetMapping gm = method.getAnnotation(GetMapping.class);
            if (gm != null) {
                EndpointInfo info = new EndpointInfo();
                info.methodName = method.getName();
                info.httpMethod = "GET";
                info.path = basePath + (gm.value().length > 0 ? gm.value()[0] : "");
                endpoints.add(info);
            }

            PostMapping pm = method.getAnnotation(PostMapping.class);
            if (pm != null) {
                EndpointInfo info = new EndpointInfo();
                info.methodName = method.getName();
                info.httpMethod = "POST";
                info.path = basePath + (pm.value().length > 0 ? pm.value()[0] : "");
                endpoints.add(info);
            }
        }
        return endpoints;
    }

    // TODO: ==================== SERVICE TEST (Akurasi 70%) ====================

    private String generateServiceTest(Class<?> clazz, String packageName, String className) {
        List<Method> publicMethods = getPublicMethods(clazz);
        List<Field> dependencies = getAutowiredFields(clazz);

        Map<String, MethodMapping> specialMappings = new HashMap<>();

        specialMappings.put("sendNotification", new MethodMapping(
                "notificationService", "sendErrorNotification", "void",
                new String[]{"ErrorContext", "AIDebugResponse", "String"}
        ));
        specialMappings.put("clearSession", new MethodMapping(
                "debugSession", "clearSession", "void",
                new String[]{"ErrorContext"}
        ));
        // analyzeError TIDAK PERLU MOCK - handle special
        specialMappings.put("generateTest", new MethodMapping(
                "unitTestGenerator", "generateUnitTest", "String",
                new String[]{"ErrorContext"}
        ));
        specialMappings.put("generateAllTests", new MethodMapping(
                "unitTestGenerator", "generateAllUnitTests", "String",
                new String[]{"String"}
        ));
        specialMappings.put("markAsFixed", new MethodMapping(
                "debugSession", "recordSuccess", "void",
                new String[]{"ErrorContext", "AIDebugResponse"}
        ));
        specialMappings.put("applyAutoFix", new MethodMapping(
                "autoFixService", "applyFix", "boolean",
                new String[]{"ErrorContext", "AIDebugResponse"}
        ));

        // Kumpulkan import
        Set<String> requiredImports = new TreeSet<>();
        requiredImports.add("org.junit.jupiter.api.Test");
        requiredImports.add("org.junit.jupiter.api.extension.ExtendWith");
        requiredImports.add("org.mockito.InjectMocks");
        requiredImports.add("org.mockito.Mock");
        requiredImports.add("org.mockito.junit.jupiter.MockitoExtension");

        for (Field field : dependencies) {
            String typeName = field.getType().getName();
            if (!typeName.startsWith("java.")) {
                requiredImports.add(typeName);
            }
        }

        for (Method method : publicMethods) {
            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive() && !returnType.getName().startsWith("java.")) {
                requiredImports.add(returnType.getName());
            }
            for (Parameter param : method.getParameters()) {
                Class<?> paramType = param.getType();
                if (!paramType.isPrimitive() && !paramType.getName().startsWith("java.")) {
                    requiredImports.add(paramType.getName());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("package %s;\n\n", packageName));

        for (String imp : requiredImports) {
            sb.append("import ").append(imp).append(";\n");
        }

        sb.append("""
        
        import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;
        
        @ExtendWith(MockitoExtension.class)
        """);

        sb.append(String.format("class %sTest {\n\n", className));
        sb.append(String.format("    @InjectMocks\n    private %s %s;\n\n", className, uncapitalize(className)));

        for (Field field : dependencies) {
            String fieldType = field.getType().getSimpleName();
            String fieldName = field.getName();
            sb.append(String.format("    @Mock\n    private %s %s;\n", fieldType, fieldName));
        }

        for (Method method : publicMethods) {
            String methodName = method.getName();

            // ========== SPECIAL CASE: analyzeError ==========
            if (methodName.equals("analyzeError")) {
                sb.append("""
                
                @Test
                void testAnalyzeError() {
                    // Arrange
                    ErrorContext errorContext = new ErrorContext();
                    errorContext.setExceptionType("NullPointerException");
                    errorContext.setMessage("Test error");
                    
                    // Act
                    AIDebugResponse result = aIDebugService.analyzeError(errorContext);
                    
                    // Assert
                    assertNotNull(result);
                }
                """);
                continue;
            }

            MethodMapping mapping = specialMappings.get(methodName);
            if (mapping == null) continue;

            String mockDependency = mapping.dependencyName;
            String mockMethodName = mapping.dependencyMethod;
            String mockReturnType = mapping.returnType;
            String[] mockParamTypes = mapping.paramTypes;
            boolean isVoid = mockReturnType.equals("void");

            Parameter[] params = method.getParameters();
            String returnType = method.getReturnType().getSimpleName();

            sb.append(String.format("""
            
                @Test
                void test%s() {
                    // Arrange
            """, capitalize(methodName)));

            // Generate parameter values
            List<String> paramNames = new ArrayList<>();
            for (Parameter p : params) {
                String paramType = p.getType().getSimpleName();
                String paramName = p.getName();
                paramNames.add(paramName);

                String mockValue = generateMockValue(p.getType());
                sb.append(String.format("        %s %s = %s;\n", paramType, paramName, mockValue));
            }

            String paramList = paramNames.isEmpty() ? "" : String.join(", ", paramNames);

            // Setup mock behavior
            if (!isVoid) {
                String mockReturn = generateMockReturnValue(mockReturnType);
                sb.append(String.format("        %s expected = %s;\n", mockReturnType, mockReturn));

                String anyMatcher = buildAnyMatcherFromTypes(mockParamTypes);
                sb.append(String.format("        when(%s.%s(%s)).thenReturn(expected);\n",
                        mockDependency, mockMethodName, anyMatcher));
            }

            sb.append(String.format("""
                
                    // Act
            """));

            if (isVoid) {
                sb.append(String.format("        %s.%s(%s);\n\n",
                        uncapitalize(className), methodName, paramList));
                sb.append("        // Assert\n");
                String anyMatcher = buildAnyMatcherFromTypes(mockParamTypes);
                sb.append(String.format("        verify(%s).%s(%s);\n",
                        mockDependency, mockMethodName, anyMatcher));
            } else {
                sb.append(String.format("        %s result = %s.%s(%s);\n\n",
                        returnType, uncapitalize(className), methodName, paramList));
                sb.append("        // Assert\n        assertNotNull(result);\n");
                String anyMatcher = buildAnyMatcherFromTypes(mockParamTypes);
                sb.append(String.format("        verify(%s).%s(%s);\n",
                        mockDependency, mockMethodName, anyMatcher));
            }

            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String buildAnyMatcherFromTypes(String[] paramTypes) {
        if (paramTypes.length == 0) return "";

        List<String> matchers = new ArrayList<>();
        for (String type : paramTypes) {
            matchers.add("any(" + type + ".class)");
        }
        return String.join(", ", matchers);
    }

    private String generateMockReturnValue(String returnType) {
        return switch (returnType) {
            case "String" -> "\"test\"";
            case "boolean", "Boolean" -> "true";
            case "int", "Integer" -> "42";
            case "long", "Long" -> "42L";
            case "AIDebugResponse" -> "new AIDebugResponse()";
            default -> "mock(" + returnType + ".class)";
        };
    }

    private String generateMockValue(Class<?> type) {
        if (type == String.class) {
            return "\"test\"";
        } else if (type == boolean.class || type == Boolean.class) {
            return "true";
        } else if (type == int.class || type == Integer.class) {
            return "42";
        } else if (type == long.class || type == Long.class) {
            return "42L";
        } else if (type == double.class || type == Double.class) {
            return "42.0";
        } else if (type.isPrimitive()) {
            return "0";
        } else {
            return "mock(" + type.getSimpleName() + ".class)";
        }
    }

    private List<Method> getPublicMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Set<String> excludeMethods = Set.of(
                "toString", "hashCode", "equals", "getClass",
                "notify", "notifyAll", "wait", "finalize"
        );

        for (Method m : clazz.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) &&
                    !m.isSynthetic() && !m.isBridge() &&
                    !excludeMethods.contains(m.getName())) {
                methods.add(m);
            }
        }
        return methods;
    }

    private List<Field> getAutowiredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Autowired.class)) {
                fields.add(f);
            }
        }
        return fields;
    }

    // TODO: ==================== REPOSITORY TEST (Akurasi 10%) ====================

    private String generateRepositoryTest(Class<?> clazz, String packageName, String className) {
        String entityClassName = extractEntityClassName(clazz);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
        package %s;
        
        import %s;
        import org.junit.jupiter.api.Test;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
        import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
        
        import java.util.List;
        import java.util.Optional;
        
        import static org.assertj.core.api.Assertions.assertThat;
        
        @DataJpaTest
        class %sTest {
            
            @Autowired
            private TestEntityManager entityManager;
            
            @Autowired
            private %s %s;
            
        """,
                packageName, entityClassName, className, className, uncapitalize(className)));

        // Generate test untuk method-method repository
        for (Method method : clazz.getDeclaredMethods()) {
            String methodName = method.getName();

            // Skip default JPA methods (sudah di-test oleh Spring)
            if (isDefaultJpaMethod(methodName)) continue;

            sb.append(generateRepositoryMethodTest(method, entityClassName, uncapitalize(className)));
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String extractEntityClassName(Class<?> repositoryClass) {
        // Cari JpaRepository<User, Long>
        for (java.lang.reflect.Type iface : repositoryClass.getGenericInterfaces()) {
            if (iface.getTypeName().contains("JpaRepository")) {
                // Extract entity class dari generic
                String typeName = iface.getTypeName();
                int start = typeName.indexOf('<') + 1;
                int end = typeName.indexOf(',');
                if (start > 0 && end > start) {
                    return typeName.substring(start, end).trim();
                }
            }
        }
        return "Object"; // Fallback
    }

    private boolean isDefaultJpaMethod(String methodName) {
        Set<String> defaultMethods = Set.of(
                "save", "saveAll", "findById", "existsById", "findAll",
                "findAllById", "count", "deleteById", "delete", "deleteAll",
                "flush", "saveAndFlush", "deleteInBatch", "deleteAllInBatch"
        );
        return defaultMethods.contains(methodName);
    }

    private String generateRepositoryMethodTest(Method method, String entityName, String repoName) {
        String methodName = method.getName();
        String returnType = method.getReturnType().getSimpleName();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
        
            @Test
            void %s_ShouldWork() {
                // Arrange
                %s entity = new %s();
                // TODO: Set required fields
                entityManager.persist(entity);
                entityManager.flush();
                
                // Act
        """, methodName, entityName, entityName));

        // Generate Act & Assert berdasarkan return type
        if (returnType.equals("Optional")) {
            sb.append(String.format("""
                Optional<%s> result = %s.%s(/* params */);
                
                // Assert
                assertThat(result).isPresent();
        """, entityName, repoName, methodName));
        } else if (returnType.equals("List")) {
            sb.append(String.format("""
                List<%s> results = %s.%s(/* params */);
                
                // Assert
                assertThat(results).isNotEmpty();
        """, entityName, repoName, methodName));
        } else if (returnType.equals("boolean") || returnType.equals("Boolean")) {
            sb.append(String.format("""
                boolean result = %s.%s(/* params */);
                
                // Assert
                assertThat(result).isTrue();
        """, repoName, methodName));
        } else {
            sb.append(String.format("""
                %s result = %s.%s(/* params */);
                
                // Assert
                assertThat(result).isNotNull();
        """, returnType, repoName, methodName));
        }

        sb.append("    }\n");
        return sb.toString();
    }

    // TODO: ==================== MODEL TEST (Akurasi 99%) ====================

    private String generateModelTest(Class<?> clazz, String packageName, String className) {
        List<String> fields = extractFieldNames(clazz);

        List<String> priorityFields = new ArrayList<>();
        String[] priority = {"exceptionType", "message", "className", "methodName", "lineNumber", "fileName"};
        for (String p : priority) {
            if (fields.contains(p)) priorityFields.add(p);
        }
        if (priorityFields.isEmpty()) {
            priorityFields = fields.subList(0, Math.min(5, fields.size()));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            package %s;
            
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.BeforeEach;
            import static org.junit.jupiter.api.Assertions.*;
            
            class %sTest {
                
                private %s obj;
                
                @BeforeEach
                void setUp() {
                    obj = new %s();
                }
            
            """, packageName, className, className, className));

        for (String field : priorityFields) {
            String capitalized = capitalize(field);
            String fieldType = getFieldType(field);
            String testValue = getTestValue(field);

            sb.append(String.format("""
                
                @Test
                void testGet%s() {
                    // Arrange
                    %s expected = %s;
                    obj.set%s(expected);
                    
                    // Act
                    %s actual = obj.get%s();
                    
                    // Assert
                    assertEquals(expected, actual);
                }
                
                @Test
                void testSet%s() {
                    // Arrange
                    %s expected = %s;
                    
                    // Act
                    obj.set%s(expected);
                    
                    // Assert
                    assertEquals(expected, obj.get%s());
                }
                """,
                    capitalized, fieldType, testValue, capitalized,
                    fieldType, capitalized,
                    capitalized, fieldType, testValue, capitalized, capitalized
            ));
        }

        sb.append("}\n");
        return sb.toString();
    }

    private List<String> extractFieldNames(Class<?> clazz) {
        Set<String> fields = new HashSet<>();
        for (Method m : clazz.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("get") && !name.equals("getClass")) {
                fields.add(uncapitalize(name.substring(3)));
            } else if (name.startsWith("is")) {
                fields.add(uncapitalize(name.substring(2)));
            }
        }
        return new ArrayList<>(fields);
    }

    private String getFieldType(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "linenumber" -> "int";
            case "timestamp" -> "LocalDateTime";
            case "stacktrace" -> "List<String>";
            default -> "String";
        };
    }

    private String getTestValue(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "linenumber" -> "42";
            case "timestamp" -> "LocalDateTime.now()";
            case "stacktrace" -> "List.of(\"line1\", \"line2\")";
            default -> "\"Test " + fieldName + "\"";
        };
    }

    // TODO: ==================== GENERIC TEST ====================

    private String generateGenericTest(String packageName, String className) {
        return String.format("""
            package %s;
            
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            class %sTest {
                
                @Test
                void testPlaceholder() {
                    assertTrue(true, "Please implement test manually");
                }
            }
            """, packageName, className);
    }

    // TODO: ==================== HELPER METHODS ====================

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private void saveTestFile(String packageName, String className, String testCode) {
        try {
            String testFilePath = "src/test/java/" + packageName.replace('.', '/') + "/" + className + "Test.java";
            Path path = Paths.get(testFilePath);
            Files.createDirectories(path.getParent());
            Files.write(path, testCode.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println(ConsoleColors.GREEN + "✅ Unit test tersimpan di: " + testFilePath + ConsoleColors.RESET);
            System.out.println(ConsoleColors.GREEN + "💡 Jalankan dengan: mvn test -Dtest=" + className + "Test" + ConsoleColors.RESET);
        } catch (IOException e) {
            System.err.println("❌ Gagal menyimpan: " + e.getMessage());
        }
    }

    // TODO: ==================== INNER CLASSES ====================

    private static class EndpointInfo {
        String methodName;
        String httpMethod;
        String path;
    }

    private static class MethodMapping {
        String dependencyName;
        String dependencyMethod;
        String returnType;
        String[] paramTypes;

        MethodMapping(String dependencyName, String dependencyMethod, String returnType, String[] paramTypes) {
            this.dependencyName = dependencyName;
            this.dependencyMethod = dependencyMethod;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }
}