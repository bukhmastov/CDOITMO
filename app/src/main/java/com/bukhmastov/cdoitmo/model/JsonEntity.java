package com.bukhmastov.cdoitmo.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

public abstract class JsonEntity implements Entity {

    private static HashMap<String, HashMap<String, EntityMetaData>> fieldsMap = new HashMap<>();

    public JsonEntity() {}


    public String toJsonString() throws JSONException, IllegalAccessException {
        return toJson().toString();
    }

    public JSONObject toJson() throws JSONException, IllegalAccessException {
        HashMap<String, EntityMetaData> fields = getFields();
        int maxOccupiedPosition = 0;
        int numberOfNonOrderedElements = 0;
        for (EntityMetaData metaData : fields.values()) {
            if (metaData.order == 0) {
                numberOfNonOrderedElements++;
                continue;
            }
            if (metaData.order > maxOccupiedPosition) {
                maxOccupiedPosition = metaData.order;
            }
        }
        List<EntityMetaDataValue> values;
        if (maxOccupiedPosition == 0) {
            values = new ArrayList<>();
        } else {
            values = new ArrayList<>(maxOccupiedPosition + numberOfNonOrderedElements);
            for (int i = 0; i < maxOccupiedPosition + numberOfNonOrderedElements; i++) {
                values.add(null);
            }
        }
        for (Field field : this.getClass().getDeclaredFields()) {
            String fieldName = field.getName();
            if (!fields.containsKey(fieldName)) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(this);
            if (value == null) {
                continue;
            }
            EntityMetaData meta = fields.get(fieldName);
            EntityMetaDataValue metaDataValue = new EntityMetaDataValue(meta, value);
            if (maxOccupiedPosition == 0) {
                values.add(metaDataValue);
                continue;
            }
            if (meta.order == 0) {
                if (maxOccupiedPosition >= values.size()) {
                    values.add(metaDataValue);
                } else {
                    values.set(maxOccupiedPosition++, metaDataValue);
                }
            } else {
                values.set(meta.order - 1, metaDataValue);
            }
        }
        JSONObject output = new JSONObject();
        for (EntityMetaDataValue metaDataValue : values) {
            if (metaDataValue == null) {
                continue;
            }
            EntityMetaData meta = metaDataValue.metaData;
            Object value = metaDataValue.value;
            if (meta.isArray) {
                Iterable iterable = (Iterable) value;
                JSONArray array = new JSONArray();
                if (meta.isDerivedFromEntity) {
                    for (Object curr : iterable) {
                        array.put(((JsonEntity) curr).toJson());
                    }
                } else {
                    for (Object curr : iterable) {
                        array.put(curr);
                    }
                }
                output.put(meta.key, array);
            } else {
                if (meta.isDerivedFromEntity) {
                    output.put(meta.key, ((JsonEntity) value).toJson());
                } else {
                    output.put(meta.key, value);
                }
            }
        }
        return output;
    }


    public <T extends JsonEntity> T fromJsonString(String json) throws JSONException, IllegalAccessException, InstantiationException {
        return fromJson(new JSONObject(json));
    }

    public <T extends JsonEntity> T fromJson(JSONObject input) throws JSONException, IllegalAccessException, InstantiationException {
        HashMap<String, EntityMetaData> fields = getFields();
        for (Field field : this.getClass().getDeclaredFields()) {
            String fieldName = field.getName();
            if (!fields.containsKey(fieldName)) {
                continue;
            }
            field.setAccessible(true);
            EntityMetaData meta = fields.get(fieldName);
            if (input.isNull(meta.key)) {
                // value is null or not present, we are skipping value
                continue;
            }
            if (!meta.isArray) {
                try {
                    if (meta.isDerivedFromEntity) {
                        JsonEntity data = castObject(input.getJSONObject(meta.key), meta.entityType);
                        if (data == null) {
                            // value is null, we are skipping value
                            continue;
                        }
                        field.set(this, data);
                    } else {
                        Object data = input.get(meta.key);
                        if (data == null || data instanceof JSONObject) {
                            // value is null or invalid, we are skipping value
                            continue;
                        }
                        field.set(this, data);
                    }
                } catch (IllegalArgumentException | JSONException ignore) {
                    // field#set(), json#getJSONObject(), probably servers returned changed value type
                    // so we are skipping changed value
                }
                continue;
            }
            JSONArray array;
            try {
                array = input.getJSONArray(meta.key);
            } catch (Throwable t) {
                array = null;
            }
            if (array == null) {
                // value is null or invalid, we are skipping value
                continue;
            }
            List list = (List) getNewInstance(meta.collectionType);
            if (meta.isDerivedFromEntity) {
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JsonEntity data = castObject(array.getJSONObject(i), meta.entityType);
                        if (data == null) {
                            // value is null, we are skipping value
                            continue;
                        }
                        list.add(data);
                    } catch (JSONException ignore) {
                        // json#getJSONObject(), probably servers returned changed value type
                        // so we are skipping changed value
                    }
                }
            } else {
                for (int i = 0; i < array.length(); i++) {
                    Object data = array.get(i);
                    if (data == null || data instanceof JSONObject) {
                        // value is null or invalid, we are skipping value
                        continue;
                    }
                    list.add(data);
                }
            }
            try {
                if (list == null) {
                    continue;
                }
                field.set(this, list);
            } catch (IllegalArgumentException ignore) {
                // field#set(), probably servers returned changed value type
                // so we are skipping changed value
            }
        }
        return (T) this;
    }


    public <T extends JsonEntity> T copy() throws JSONException, IllegalAccessException, InstantiationException {
        return getClass().newInstance().fromJson(toJson());
    }


    private <T extends JsonEntity> T castObject(JSONObject json, Class<T> type) throws JSONException, IllegalAccessException, InstantiationException {
        if (json == null) {
            return null;
        }
        T entity = type.newInstance();
        entity.fromJson(json);
        return entity;
    }

    private <T> T getNewInstance(Class<T> type) throws IllegalAccessException, InstantiationException {
        return type.newInstance();
    }

    private @NonNull HashMap<String, EntityMetaData> getFields() {
        HashMap<String, EntityMetaData> fields = fieldsMap.get(this.getClass().getName());
        if (fields == null || fields.size() == 0) {
            bind(this);
            fields = fieldsMap.get(this.getClass().getName());
            if (fields == null || fields.size() == 0) {
                throw new IllegalStateException("JsonEntity#getFields() failed to bind entity");
            }
        }
        return fields;
    }

    private void bind(JsonEntity entity) {
        try {
            String className = entity.getClass().getName();
            if (!fieldsMap.containsKey(className)) {
                fieldsMap.put(className, new HashMap<>());
            }
            HashMap<String, EntityMetaData> fields = fieldsMap.get(className);
            for (Field field : entity.getClass().getDeclaredFields()) {
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                if (jsonProperty == null) {
                    continue;
                }
                String key = jsonProperty.value();
                int order = jsonProperty.order() < 0 ? 0 : jsonProperty.order();
                Class<?> fieldType = field.getType();
                //if (Iterable.class.isAssignableFrom(fieldType)) {
                if (List.class.isAssignableFrom(fieldType)) {
                    Type type = field.getGenericType();
                    ParameterizedType pt = (ParameterizedType) type;
                    Type collectionType = pt.getActualTypeArguments()[0];
                    fields.put(field.getName(), new EntityMetaData(key, order, (Class<?>) collectionType, fieldType));
                    continue;
                }
                fields.put(field.getName(), new EntityMetaData(key, order, fieldType));
            }
            fieldsMap.put(className, fields);
        } catch (Throwable throwable) {
            android.util.Log.e("JsonEntity", throwable.getMessage(), throwable);
        }
    }

    private class EntityMetaData {
        private EntityMetaData(String key, int order, Class entityType, Class<?> collectionType) {
            this.key = key;
            this.order = order;
            this.entityType = entityType;
            this.collectionType = collectionType;
            this.isArray = true;
            this.isDerivedFromEntity = JsonEntity.class.isAssignableFrom(entityType);
        }
        private EntityMetaData(String key, int order, Class entityType) {
            this.key = key;
            this.order = order;
            this.entityType = entityType;
            this.collectionType = void.class;
            this.isArray = false;
            this.isDerivedFromEntity = JsonEntity.class.isAssignableFrom(entityType);
        }
        private String key;
        private int order;
        private Class entityType;
        private Class<?> collectionType;
        private boolean isArray;
        private boolean isDerivedFromEntity;
    }
    private class EntityMetaDataValue {
        public EntityMetaDataValue(EntityMetaData metaData, Object value) {
            this.metaData = metaData;
            this.value = value;
        }
        private EntityMetaData metaData;
        private Object value;
    }
}
