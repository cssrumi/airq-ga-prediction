package pl.airq.prediction.ga.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;

class FieldMap<T> {

    private final Map<String, Field> fieldMap;
    private final Class<T> phenotypeClass;

    FieldMap(Class<T> phenotypeClass) {
        this.phenotypeClass = phenotypeClass;
        this.fieldMap = prepareFieldMap(phenotypeClass);
    }

    Float getFloat(String field, T object) {
        return (Float) get(field, object);
    }

    Object get(String field, T object) {
        try {
            final Field result = fieldMap.get(field);
            return result != null ? result.get(object) : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static <T> Map<String, Field> prepareFieldMap(Class<T> clazz) {
        return FieldUtils.getAllFieldsList(clazz)
                         .stream()
                         .filter(field -> !Modifier.isStatic(field.getModifiers()))
                         .filter(field -> Modifier.isPublic(field.getModifiers()))
                         .collect(Collectors.toUnmodifiableMap(Field::getName, field -> field));
    }
}
