/*
 * SchemaFactory.java
 *
 * Created on August 18, 2003, 4:20 PM
 */

package org.gk.schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author  vastrik
 */
public class SchemaParser {
        
    private Map cache = new HashMap();
    
    public GKSchema parseResultSet(ResultSet rs) throws SQLException, java.lang.ClassNotFoundException {
        while(rs.next()) {
            /**
            System.err.println(rs.getString(1) + "\t" +
                               rs.getString(2) + "\t" +
                               rs.getString(3) + "\t" +
                               rs.getString(4) + "\t" +
                               rs.getString(5) + "\t" +
                               rs.getString(6)
                               );
             */
            String className = rs.getString(2);
            if (className.equals("SchemaClassAttribute")) {
                handleSchemaAttribute(rs);
            } else if (className.equals("SchemaClass")) {
                handleSchemaClass(rs);
            } else if (className.equals("Schema")) {
                handleSchema(rs);
            } else {
            	System.err.println("Don't know how to handle " + className);
            }
        }
        GKSchema s = (GKSchema) cache.get("Schema");
        s.setCache(cache);
        s.initialise();
        return s;
    }

    private void handleSchemaAttribute(ResultSet rs) throws SQLException, java.lang.ClassNotFoundException {
        GKSchemaAttribute s = schemaAttributeFromCacheOrNew(rs.getString(1));
        String propertyName = rs.getString(3);
        if (propertyName.equals("name")) {
            s.setName(rs.getString(4));
        } else if (propertyName.equals("class")) {
            s.addSchemaClass(schemaClassFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("value_defines_instance")) {
            s.setDefiningType(rs.getString(4));
        } else if (propertyName.equals("max_cardinality")) {
            if (rs.getString(4) != null) {
                s.setMaxCardinality(rs.getInt(4));
            }
        } else if (propertyName.equals("min_cardinality")) {
             if (rs.getString(4) != null) {
                s.setMinCardinality(rs.getInt(4));
            }       
        } else if (propertyName.equals("type")) {
            String type = rs.getString(4);
            if (type.equals("db_instance_type")) {
                s.setType(Class.forName("org.gk.model.Instance"));
                s.setTypeAsInt(SchemaAttribute.INSTANCE_TYPE);
            } else if (type.equals("db_string_type")) {
                s.setType(Class.forName("java.lang.String"));
				s.setTypeAsInt(SchemaAttribute.STRING_TYPE);
            } else if (type.equals("db_integer_type")) {
                s.setType(Class.forName("java.lang.Integer"));
				s.setTypeAsInt(SchemaAttribute.INTEGER_TYPE);
            } else if (type.equals("db_enum_type")) {
                if (s.getType() == null) { // To avoid overwriting the case in the db_col_type
                    s.setType(Class.forName("java.lang.Boolean"));
                    s.setTypeAsInt(SchemaAttribute.BOOLEAN_TYPE);
                }
            } else if (type.equals("db_float_type")) {
                s.setType(Class.forName("java.lang.Float"));
				s.setTypeAsInt(SchemaAttribute.FLOAT_TYPE);
			} else if (type.equals("db_long_type")) {
				s.setType(Class.forName("java.lang.Long"));
				s.setTypeAsInt(SchemaAttribute.LONG_TYPE);
            } else {
                //System.err.println("Ignored SchemaAttribute type " + type);
            }
        } else if (propertyName.equals("db_col_type")) {
            // Check if there is a list of allowed values provided
            String value = rs.getString(4);
            if (value != null && !value.equals("NULL") && value.startsWith("ENUM")) {
                value = value.substring(5, value.length() - 1); // Remove enum( and ).
                String[] tokens = value.split(",");
                if (tokens.length > 2) { // This is a simple implementation: if two values provided, it will be assumed as boolean type.
                    s.setTypeAsInt(SchemaAttribute.ENUM_TYPE);
                    s.setType(Enum.class);
                    List<String> allowedValues = new ArrayList<String>();
                    s.setAllowedValues(allowedValues);
                    for (String token : tokens) {
                        token = token.trim();
                        allowedValues.add(token.substring(1, token.length() - 1));
                    }
                }
            }
        } else if (propertyName.equals("multiple")) {
            s.setMultiple(rs.getBoolean(4));
        } else if (propertyName.equals("allowed_classes")) {
            s.addAllowedClass(schemaClassFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("inverse_slots")) {
            s.setInverseSchemaAttribute(schemaAttributeFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("default")) {
            if (s.getTypeAsInt() == SchemaAttribute.INTEGER_TYPE) {
            		s.setDefaultValue(new Integer(rs.getInt(4)));
            } else if (s.getTypeAsInt() == SchemaAttribute.FLOAT_TYPE) {
        			s.setDefaultValue(new Float(rs.getFloat(4)));
            } else if (s.getTypeAsInt() == SchemaAttribute.BOOLEAN_TYPE) {
            		s.setDefaultValue(new Boolean(rs.getBoolean(4)));
            } else {
            		s.setDefaultValue(new String(rs.getString(4)));
            }
        } else if (propertyName.equals("category")) {
        		s.setCategory(rs.getString(4));
        } else {
            //System.err.println("Ignored SchemaAttribute property " + propertyName);
        }
    }
    
    private void handleSchemaClass(ResultSet rs) throws SQLException {
        GKSchemaClass s = schemaClassFromCacheOrNew(rs.getString(1));
        String propertyName = rs.getString(3);
        //if (propertyName.equals("attributes")) {
        //    s.addAttribute(schemaAttributeFromCacheOrNew(rs.getString(4)));
        //} else
        if (propertyName.equals("super_classes")) {
            s.addSuperClass(schemaClassFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("abstract")) {
            s.setAbstract(rs.getBoolean(4));
        } else if (propertyName.equals("name")) {
            s.setName(rs.getString(4));
        } else {
            //System.err.println("Ignored SchemaClass property " + propertyName);
        }
    }
    
    private void handleSchema(ResultSet rs) throws SQLException {
        GKSchema s = schemaFromCacheOrNew(rs.getString(1));
        String propertyName = rs.getString(3);
        if (propertyName.equals("classes")) {
            s.addClass(schemaClassFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("attributes")) {
            s.addAttribute(schemaAttributeFromCacheOrNew(rs.getString(4)));
        } else if (propertyName.equals("_timestamp")) {
        	s.setTimestamp(rs.getString(4));
        } else {
            //System.err.println("Ignored Schema property " + propertyName);
        }
    }
    
    private GKSchemaAttribute schemaAttributeFromCacheOrNew(String id) {
        GKSchemaAttribute s;
        if ((s = (GKSchemaAttribute) cache.get(id)) != null) {
            return s;
        } else {
            s = new GKSchemaAttribute(id);
            cache.put(id, s);
            return s;
        }
    }
    
    private GKSchemaClass schemaClassFromCacheOrNew(String id) {
        GKSchemaClass s;
        if ((s = (GKSchemaClass) cache.get(id)) != null) {
            return s;
        } else {
            s = new GKSchemaClass(id);
            cache.put(id, s);
            return s;
        }
    }
    
    private GKSchema schemaFromCacheOrNew(String id) {
        GKSchema s;
        if ((s = (GKSchema) cache.get(id)) != null) {
            return s;
        } else {
            s = new GKSchema();
            cache.put(id, s);
            return s;
        }
    }
    

}
