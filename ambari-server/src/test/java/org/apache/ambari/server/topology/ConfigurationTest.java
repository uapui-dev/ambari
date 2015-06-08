/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Configuration unit tests.
 */
public class ConfigurationTest {

  private static final Map<String, Map<String, String>> EMPTY_PROPERTIES = new HashMap<String, Map<String, String>>();
  private static final Map<String, Map<String, Map<String, String>>>  EMPTY_ATTRIBUTES = new HashMap<String, Map<String, Map<String, String>>>();

  @Test
  public void testGetProperties_noParent() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProperties1 = new HashMap<String, String>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<String, String>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES);
    assertEquals(properties, configuration.getProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetFullProperties_noParent() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProperties1 = new HashMap<String, String>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<String, String>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES);
    assertEquals(properties, configuration.getFullProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetProperties_withParent() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProperties1 = new HashMap<String, String>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<String, String>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Map<String, Map<String, String>> parentProperties = new HashMap<String, Map<String, String>>();
    Map<String, String> parentTypeProperties1 = new HashMap<String, String>();
    parentTypeProperties1.put("prop5", "val5");
    Map<String, String> parentTypeProperties3 = new HashMap<String, String>();
    parentTypeProperties3.put("prop6", "val6");

    parentProperties.put("type1", parentTypeProperties1);
    parentProperties.put("type3", parentTypeProperties3);

    Configuration parentConfiguration = new Configuration(parentProperties, EMPTY_ATTRIBUTES);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES, parentConfiguration);
    // parent should not be reflected in getProperties() result
    assertEquals(properties, configuration.getProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetFullProperties_withParent() {
    Configuration configuration = createConfigurationWithParentsPropsOnly();
    // get prop maps prior to calling getFullProperties
    Map<String, Map<String, String>> leafProperties = configuration.getProperties();
    Map<String, Map<String, String>> parentProperties = configuration.getParentConfiguration().getProperties();
    Map<String, Map<String, String>> parentParentProperties = configuration.getParentConfiguration().getParentConfiguration().getProperties();

    // test
    // all parents should be reflected in getFullProperties() result
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties();

    // type1, type2, type3, type4
    assertEquals(4, fullProperties.size());
    // type1
    Map<String, String> type1Props = fullProperties.get("type1");
    assertEquals(5, type1Props.size());
    assertEquals("val1.3", type1Props.get("prop1"));
    assertEquals("val2.2", type1Props.get("prop2"));
    assertEquals("val3.1", type1Props.get("prop3"));
    assertEquals("val6.2", type1Props.get("prop6"));
    assertEquals("val9.3", type1Props.get("prop9"));

    //type2
    Map<String, String> type2Props = fullProperties.get("type2");
    assertEquals(2, type2Props.size());
    assertEquals("val4.3", type2Props.get("prop4"));
    assertEquals("val5.1", type2Props.get("prop5"));

    //type3
    Map<String, String> type3Props = fullProperties.get("type3");
    assertEquals(2, type3Props.size());
    assertEquals("val7.3", type3Props.get("prop7"));
    assertEquals("val8.2", type3Props.get("prop8"));

    //type4
    Map<String, String> type4Props = fullProperties.get("type4");
    assertEquals(2, type4Props.size());
    assertEquals("val10.3", type4Props.get("prop10"));
    assertEquals("val11.3", type4Props.get("prop11"));

    // ensure that underlying property map is not modified in getFullProperties
    assertEquals(leafProperties, configuration.getProperties());
    assertEquals(parentProperties, configuration.getParentConfiguration().getProperties());
    assertEquals(parentParentProperties, configuration.getParentConfiguration().getParentConfiguration().getProperties());

    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());

    Collection<String> configTypes = configuration.getAllConfigTypes();
    assertEquals(4, configTypes.size());
    assertTrue(configTypes.containsAll(Arrays.asList("type1", "type2", "type3", "type4")));
  }

  @Test
  public void testGetFullProperties_withParent_specifyDepth() {
    Configuration configuration = createConfigurationWithParentsPropsOnly();
    // get prop maps prior to calling getFullProperties
    Map<String, Map<String, String>> leafProperties = configuration.getProperties();
    Map<String, Map<String, String>> parentProperties = configuration.getParentConfiguration().getProperties();
    Map<String, Map<String, String>> parentParentProperties = configuration.getParentConfiguration().getParentConfiguration().getProperties();

    // test
    // specify a depth of 1 which means to include only 1 level up the parent chain
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties(1);

    // type1, type2, type3, type4
    assertEquals(4, fullProperties.size());
    // type1
    Map<String, String> type1Props = fullProperties.get("type1");
    assertEquals(4, type1Props.size());
    assertEquals("val1.3", type1Props.get("prop1"));
    assertEquals("val2.2", type1Props.get("prop2"));
    assertEquals("val6.2", type1Props.get("prop6"));
    assertEquals("val9.3", type1Props.get("prop9"));

    //type2
    Map<String, String> type2Props = fullProperties.get("type2");
    assertEquals(1, type2Props.size());
    assertEquals("val4.3", type2Props.get("prop4"));

    //type3
    Map<String, String> type3Props = fullProperties.get("type3");
    assertEquals(2, type3Props.size());
    assertEquals("val7.3", type3Props.get("prop7"));
    assertEquals("val8.2", type3Props.get("prop8"));

    //type4
    Map<String, String> type4Props = fullProperties.get("type4");
    assertEquals(2, type4Props.size());
    assertEquals("val10.3", type4Props.get("prop10"));
    assertEquals("val11.3", type4Props.get("prop11"));

    // ensure that underlying property maps are not modified in getFullProperties
    assertEquals(leafProperties, configuration.getProperties());
    assertEquals(parentProperties, configuration.getParentConfiguration().getProperties());
    assertEquals(parentParentProperties, configuration.getParentConfiguration().getParentConfiguration().getProperties());

    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetAttributes_noParent() {
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> attributeProperties = new HashMap<String, Map<String, String>>();
    Map<String, String> properties1 = new HashMap<String, String>();
    properties1.put("prop1", "val1");
    properties1.put("prop2", "val2");
    Map<String, String> properties2 = new HashMap<String, String>();
    properties2.put("prop1", "val3");
    attributeProperties.put("attribute1", properties1);
    attributeProperties.put("attribute2", properties2);

    attributes.put("type1", attributeProperties);

    //test
    Configuration configuration = new Configuration(EMPTY_PROPERTIES, attributes);
    // assert attributes
    assertEquals(attributes, configuration.getAttributes());
    // assert empty properties
    assertEquals(EMPTY_PROPERTIES, configuration.getProperties());
  }

  @Test
  public void testGetFullAttributes_withParent() {
    Configuration configuration = createConfigurationWithParentsAttributesOnly();
    Map<String, Map<String, Map<String, String>>> leafAttributes = configuration.getAttributes();
    Map<String, Map<String, Map<String, String>>> parentAttributes = configuration.getParentConfiguration().getAttributes();
    Map<String, Map<String, Map<String, String>>> parentParentAttributes = configuration.getParentConfiguration().getParentConfiguration().getAttributes();
    // test
    // all parents should be reflected in getFullAttributes() result
    Map<String, Map<String, Map<String, String>>> fullAttributes = configuration.getFullAttributes();
    assertEquals(2, fullAttributes.size());

    // type 1
    Map<String, Map<String, String>> type1Attributes = fullAttributes.get("type1");
    // attribute1, attribute2, attribute3, attribute4
    assertEquals(4, type1Attributes.size());
    // attribute1
    Map<String, String> attribute1Properties = type1Attributes.get("attribute1");
    assertEquals(5, attribute1Properties.size());
    assertEquals("val1.3", attribute1Properties.get("prop1"));
    assertEquals("val2.2", attribute1Properties.get("prop2"));
    assertEquals("val3.1", attribute1Properties.get("prop3"));
    assertEquals("val6.2", attribute1Properties.get("prop6"));
    assertEquals("val9.3", attribute1Properties.get("prop9"));

    //attribute2
    Map<String, String> attribute2Properties = type1Attributes.get("attribute2");
    assertEquals(2, attribute2Properties.size());
    assertEquals("val4.3", attribute2Properties.get("prop4"));
    assertEquals("val5.1", attribute2Properties.get("prop5"));

    //attribute3
    Map<String, String> attribute3Properties = type1Attributes.get("attribute3");
    assertEquals(2, attribute3Properties.size());
    assertEquals("val7.3", attribute3Properties.get("prop7"));
    assertEquals("val8.2", attribute3Properties.get("prop8"));

    //attribute4
    Map<String, String> attribute4Properties = type1Attributes.get("attribute4");
    assertEquals(2, attribute4Properties.size());
    assertEquals("val10.3", attribute4Properties.get("prop10"));
    assertEquals("val11.3", attribute4Properties.get("prop11"));

    // type 2
    Map<String, Map<String, String>> type2Attributes = fullAttributes.get("type2");
    // attribute100, attribute101
    assertEquals(2, type2Attributes.size());

    Map<String, String> attribute100Properties = type2Attributes.get("attribute100");
    assertEquals(3, attribute100Properties.size());
    assertEquals("val100.3", attribute100Properties.get("prop100"));
    assertEquals("val101.1", attribute100Properties.get("prop101"));
    assertEquals("val102.3", attribute100Properties.get("prop102"));

    Map<String, String> attribute101Properties = type2Attributes.get("attribute101");
    assertEquals(2, attribute101Properties.size());
    assertEquals("val100.2", attribute101Properties.get("prop100"));
    assertEquals("val101.1", attribute101Properties.get("prop101"));

    // ensure that underlying attribute maps are not modified in getFullProperties
    assertEquals(leafAttributes, configuration.getAttributes());
    assertEquals(parentAttributes, configuration.getParentConfiguration().getAttributes());
    assertEquals(parentParentAttributes, configuration.getParentConfiguration().getParentConfiguration().getAttributes());

    assertEquals(EMPTY_PROPERTIES, configuration.getProperties());

    Collection<String> configTypes = configuration.getAllConfigTypes();
    assertEquals(2, configTypes.size());
    assertTrue(configTypes.containsAll(Arrays.asList("type1", "type2")));
  }

  @Test
  public void testGetPropertyValue() {
    Configuration configuration = createConfigurationWithParentsPropsOnly();

    assertEquals("val1.3", configuration.getPropertyValue("type1", "prop1"));
    assertEquals("val2.2", configuration.getPropertyValue("type1", "prop2"));
    assertEquals("val3.1", configuration.getPropertyValue("type1", "prop3"));
    assertEquals("val4.3", configuration.getPropertyValue("type2", "prop4"));
    assertEquals("val5.1", configuration.getPropertyValue("type2", "prop5"));
    assertEquals("val6.2", configuration.getPropertyValue("type1", "prop6"));
    assertEquals("val7.3", configuration.getPropertyValue("type3", "prop7"));
    assertEquals("val8.2", configuration.getPropertyValue("type3", "prop8"));
    assertEquals("val10.3", configuration.getPropertyValue("type4", "prop10"));
    assertEquals("val11.3", configuration.getPropertyValue("type4", "prop11"));
  }

  @Test
  public void testGetAttributeValue() {
    Configuration configuration = createConfigurationWithParentsAttributesOnly();

    assertEquals("val1.3", configuration.getAttributeValue("type1", "prop1", "attribute1"));
    assertEquals("val2.2", configuration.getAttributeValue("type1", "prop2", "attribute1"));
    assertEquals("val3.1", configuration.getAttributeValue("type1", "prop3", "attribute1"));
    assertEquals("val4.3", configuration.getAttributeValue("type1", "prop4", "attribute2"));
    assertEquals("val5.1", configuration.getAttributeValue("type1", "prop5", "attribute2"));
    assertEquals("val6.2", configuration.getAttributeValue("type1", "prop6", "attribute1"));
    assertEquals("val7.3", configuration.getAttributeValue("type1", "prop7", "attribute3"));
    assertEquals("val8.2", configuration.getAttributeValue("type1", "prop8", "attribute3"));
    assertEquals("val100.3", configuration.getAttributeValue("type2", "prop100", "attribute100"));
    assertEquals("val101.1", configuration.getAttributeValue("type2", "prop101", "attribute100"));
    assertEquals("val102.3", configuration.getAttributeValue("type2", "prop102", "attribute100"));
    assertEquals("val100.2", configuration.getAttributeValue("type2", "prop100", "attribute101"));
    assertEquals("val101.1", configuration.getAttributeValue("type2", "prop101", "attribute101"));
  }

  private Configuration createConfigurationWithParentsPropsOnly() {
    // parents parent config properties
    Map<String, Map<String, String>> parentParentProperties = new HashMap<String, Map<String, String>>();
    Map<String, String> parentParentTypeProperties1 = new HashMap<String, String>();
    parentParentTypeProperties1.put("prop1", "val1.1");
    parentParentTypeProperties1.put("prop2", "val2.1");
    parentParentTypeProperties1.put("prop3", "val3.1");
    Map<String, String> parentParentTypeProperties2 = new HashMap<String, String>();
    parentParentTypeProperties2.put("prop4", "val4.1");
    parentParentTypeProperties2.put("prop5", "val5.1");

    parentParentProperties.put("type1", parentParentTypeProperties1);
    parentParentProperties.put("type2", parentParentTypeProperties2);
    Configuration parentParentConfiguration = new Configuration(parentParentProperties, EMPTY_ATTRIBUTES);

    // parent config properties
    Map<String, Map<String, String>> parentProperties = new HashMap<String, Map<String, String>>();
    Map<String, String> parentTypeProperties1 = new HashMap<String, String>(); // override
    parentTypeProperties1.put("prop1", "val1.2"); // override parent
    parentTypeProperties1.put("prop2", "val2.2"); // override parent
    parentTypeProperties1.put("prop6", "val6.2"); // new
    Map<String, String> parentTypeProperties3 = new HashMap<String, String>(); // new
    parentTypeProperties3.put("prop7", "val7.2"); // new
    parentTypeProperties3.put("prop8", "val8.2"); // new

    parentProperties.put("type1", parentTypeProperties1);
    parentProperties.put("type3", parentTypeProperties3);
    Configuration parentConfiguration = new Configuration(parentProperties, EMPTY_ATTRIBUTES, parentParentConfiguration);

    // leaf config properties
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProperties1 = new HashMap<String, String>();
    typeProperties1.put("prop1", "val1.3"); // overrides both parent and parents parent
    typeProperties1.put("prop9", "val9.3"); // new
    Map<String, String> typeProperties2 = new HashMap<String, String>(); // overrides
    typeProperties2.put("prop4", "val4.3"); // overrides parents parent value
    Map<String, String> typeProperties3 = new HashMap<String, String>(); // overrides
    typeProperties3.put("prop7", "val7.3"); // overrides parents parent value
    Map<String, String> typeProperties4 = new HashMap<String, String>(); // new
    typeProperties4.put("prop10", "val10.3"); // new
    typeProperties4.put("prop11", "val11.3"); // new

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);
    properties.put("type3", typeProperties3);
    properties.put("type4", typeProperties4);
    return new Configuration(properties, EMPTY_ATTRIBUTES, parentConfiguration);
  }

  private Configuration createConfigurationWithParentsAttributesOnly() {
    // parents parent config attributes.
    Map<String, Map<String, Map<String, String>>> parentParentAttributes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> parentParentTypeAttributes1 = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> parentParentTypeAttributes2 = new HashMap<String, Map<String, String>>();
    parentParentAttributes.put("type1", parentParentTypeAttributes1);
    parentParentAttributes.put("type2", parentParentTypeAttributes2);

    Map<String, String> parentParentAttributeProperties1 = new HashMap<String, String>();
    parentParentAttributeProperties1.put("prop1", "val1.1");
    parentParentAttributeProperties1.put("prop2", "val2.1");
    parentParentAttributeProperties1.put("prop3", "val3.1");
    Map<String, String> parentParentAttributeProperties2 = new HashMap<String, String>();
    parentParentAttributeProperties2.put("prop4", "val4.1");
    parentParentAttributeProperties2.put("prop5", "val5.1");

    parentParentTypeAttributes1.put("attribute1", parentParentAttributeProperties1);
    parentParentTypeAttributes1.put("attribute2", parentParentAttributeProperties2);

    Map<String, String> parentParentAttributeProperties100 = new HashMap<String, String>();
    parentParentAttributeProperties100.put("prop100", "val100.1");
    parentParentAttributeProperties100.put("prop101", "val101.1");

    Map<String, String> parentParentAttributeProperties101 = new HashMap<String, String>();
    parentParentAttributeProperties101.put("prop100", "val100.1");
    parentParentAttributeProperties101.put("prop101", "val101.1");

    parentParentTypeAttributes2.put("attribute100", parentParentAttributeProperties100);
    parentParentTypeAttributes2.put("attribute101", parentParentAttributeProperties101);
    Configuration parentParentConfiguration = new Configuration(EMPTY_PROPERTIES,
        new HashMap<String, Map<String, Map<String, String>>>(parentParentAttributes));

    // parent config attributes
    Map<String, Map<String, Map<String, String>>> parentAttributes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> parentTypeAttributes1 = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> parentTypeAttributes2 = new HashMap<String, Map<String, String>>();
    parentAttributes.put("type1", parentTypeAttributes1);
    parentAttributes.put("type2", parentTypeAttributes2);

    Map<String, String> parentAttributeProperties1 = new HashMap<String, String>(); // override
    parentAttributeProperties1.put("prop1", "val1.2"); // override parent
    parentAttributeProperties1.put("prop2", "val2.2"); // override parent
    parentAttributeProperties1.put("prop6", "val6.2"); // new
    Map<String, String> parentAttributeProperties3 = new HashMap<String, String>(); // new
    parentAttributeProperties3.put("prop7", "val7.2"); // new
    parentAttributeProperties3.put("prop8", "val8.2"); // new

    parentTypeAttributes1.put("attribute1", parentAttributeProperties1);
    parentTypeAttributes1.put("attribute3", parentAttributeProperties3);

    Map<String, String> parentAttributeProperties101 = new HashMap<String, String>();
    parentAttributeProperties101.put("prop100", "val100.2");
    parentTypeAttributes2.put("attribute101", parentAttributeProperties101);
    Configuration parentConfiguration = new Configuration(EMPTY_PROPERTIES,
        new HashMap<String, Map<String, Map<String, String>>>(parentAttributes), parentParentConfiguration);

    // leaf config attributes
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> typeAttributes1 = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> typeAttributes2 = new HashMap<String, Map<String, String>>();
    attributes.put("type1", typeAttributes1);
    attributes.put("type2", typeAttributes2);

    Map<String, String> attributeProperties1 = new HashMap<String, String>();
    attributeProperties1.put("prop1", "val1.3"); // overrides both parent and parents parent
    attributeProperties1.put("prop9", "val9.3"); // new
    Map<String, String> attributeProperties2 = new HashMap<String, String>(); // overrides
    attributeProperties2.put("prop4", "val4.3"); // overrides parents parent value
    Map<String, String> attributeProperties3 = new HashMap<String, String>(); // overrides
    attributeProperties3.put("prop7", "val7.3"); // overrides parents parent value
    Map<String, String> attributeProperties4 = new HashMap<String, String>(); // new
    attributeProperties4.put("prop10", "val10.3"); // new
    attributeProperties4.put("prop11", "val11.3"); // new

    typeAttributes1.put("attribute1", attributeProperties1);
    typeAttributes1.put("attribute2", attributeProperties2);
    typeAttributes1.put("attribute3", attributeProperties3);
    typeAttributes1.put("attribute4", attributeProperties4);

    Map<String, String> attributeProperties100 = new HashMap<String, String>(); // overrides parents parent
    attributeProperties100.put("prop100", "val100.3"); // overrides parents parent
    attributeProperties100.put("prop102", "val102.3"); // new

    typeAttributes1.put("attribute1", attributeProperties1);
    typeAttributes2.put("attribute100", attributeProperties100);
    return new Configuration(EMPTY_PROPERTIES,
        new HashMap<String, Map<String, Map<String, String>>>(attributes), parentConfiguration);
  }
}