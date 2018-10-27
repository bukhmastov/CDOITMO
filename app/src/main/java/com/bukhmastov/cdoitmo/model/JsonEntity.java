package com.bukhmastov.cdoitmo.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public abstract class JsonEntity implements Entity {

    private static HashMap<String, HashMap<String, EntityMetaData>> fieldsMap = new HashMap<>();

    public JsonEntity() {}


    public String toJsonString() throws JSONException, IllegalAccessException {
        return toJson().toString();
    }

    public JSONObject toJson() throws JSONException, IllegalAccessException {
        HashMap<String, EntityMetaData> fields = getFields();
        JSONObject output = new JSONObject();
        for (Field field : this.getClass().getDeclaredFields()) {
            String fieldName = field.getName();
            if (!fields.containsKey(fieldName)) {
                continue;
            }
            field.setAccessible(true);
            EntityMetaData meta = fields.get(fieldName);
            Object fieldValue = field.get(this);
            if (fieldValue == null) {
                continue;
            }
            if (meta.isArray) {
                Iterable iterable = (Iterable) fieldValue;
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
                    output.put(meta.key, ((JsonEntity) fieldValue).toJson());
                } else {
                    output.put(meta.key, fieldValue);
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
                final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                if (jsonProperty == null) {
                    continue;
                }
                String filedProperty = jsonProperty.value();
                Class<?> fieldType = field.getType();
                //if (!Iterable.class.isAssignableFrom(fieldType)) {
                if (!List.class.isAssignableFrom(fieldType)) {
                    fields.put(field.getName(), new EntityMetaData(filedProperty, fieldType));
                    continue;
                }
                Type type = field.getGenericType();
                ParameterizedType pt = (ParameterizedType) type;
                Type innerType = pt.getActualTypeArguments()[0];
                fields.put(field.getName(), new EntityMetaData(filedProperty, (Class<?>) innerType, fieldType));
            }
            fieldsMap.put(className, fields);
        } catch (Throwable throwable) {
            android.util.Log.e("JsonEntity", throwable.getMessage(), throwable);
        }
    }

    private class EntityMetaData {
        private EntityMetaData(String key, Class entityType, Class collectionType) {
            this.key = key;
            this.entityType = entityType;
            this.collectionType = collectionType;
            this.isArray = true;
            this.isDerivedFromEntity = JsonEntity.class.isAssignableFrom(entityType);
        }
        private EntityMetaData(String key, Class entityType) {
            this.key = key;
            this.entityType = entityType;
            this.collectionType = void.class;
            this.isArray = false;
            this.isDerivedFromEntity = JsonEntity.class.isAssignableFrom(entityType);
        }
        private String key;
        private Class entityType;
        private Class<?> collectionType;
        private boolean isArray;
        private boolean isDerivedFromEntity;
    }
}
