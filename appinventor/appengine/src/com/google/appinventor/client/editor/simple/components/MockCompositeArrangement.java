// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2015 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.youngandroid.YaFormEditor;
import com.google.appinventor.client.editor.youngandroid.properties.YoungAndroidHorizontalAlignmentChoicePropertyEditor;
import com.google.appinventor.client.editor.youngandroid.properties.YoungAndroidVerticalAlignmentChoicePropertyEditor;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.properties.json.ClientJsonParser;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.shared.properties.json.JSONArray;
import com.google.appinventor.shared.properties.json.JSONObject;
import com.google.appinventor.shared.properties.json.JSONValue;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;

import com.google.common.collect.Lists;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.List;

/**
 * It is basically a MockVerticalArrangement but with special configurations.
 */
public final class MockCompositeArrangement extends MockHVArrangement {

  private String type;
  private String name;
  private JSONObject composition;
  private JSONObject propertiesMap;

  private static final String H_A = PROPERTY_NAME_HORIZONTAL_ALIGNMENT;
  private static final String V_A = PROPERTY_NAME_VERTICAL_ALIGNMENT;
  private static final String H_A_DEFAULT =
      ComponentConstants.HORIZONTAL_ALIGNMENT_DEFAULT + "";
  private static final String V_A_DEFAULT =
      ComponentConstants.VERTICAL_ALIGNMENT_DEFAULT + "";

  /**
   * Creates a new MockCompositeArrangement component.
   *
   * @param editor  editor of source file the component belongs to
   * @param type  the type this MockCompositeArrangement represents
   * @param name  the displayed name of the type
   */
  public MockCompositeArrangement(SimpleEditor editor, String type, String name) {
    super(editor, name, images.extension(),
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL);

    this.type = type;
    this.name = name;

    initPropertiesMap();

    addProperty(H_A, H_A_DEFAULT, H_A,
        new YoungAndroidHorizontalAlignmentChoicePropertyEditor());
    addProperty(V_A, V_A_DEFAULT, V_A,
        new YoungAndroidVerticalAlignmentChoicePropertyEditor());
  }

  public void onCreateFromPalette() {
    if (composition != null) {
      buildDescendentTree(composition);
      return;
    }

    Ode ode = Ode.getInstance();
    long projectId = ode.getCurrentYoungAndroidProjectId();
    String fileId = "assets/external_comps/" + type + "/composition.scmx";
    OdeAsyncCallback<ChecksumedLoadFile> callback =
        new OdeAsyncCallback<ChecksumedLoadFile>() {
          @Override
          public void onSuccess(ChecksumedLoadFile result) {
            String content;
            try {
              content = result.getContent();
            } catch (ChecksumedFileException e) {
              onFailure(e);
              return;
            }

            if (getContainer() == null) {
              // this handles the weird situation that this method is called
              // twice when it is dropped to the screen that in the first call,
              // getContainer() yields null
              return;
            }

            composition = YoungAndroidSourceAnalyzer.parseSourceFile(content,
                new ClientJsonParser()).asObject();

            buildDescendentTree(composition);
          }
        };

    ode.getProjectService().load2(projectId, fileId, callback);
  }

  @Override
  protected TreeItem buildTree() {
    TreeItem compositeArrangementNode = super.buildTree();
    compositeArrangementNode.removeItems();
    return compositeArrangementNode;
  }

  @Override
  protected boolean isPropertyVisible(String propertyName) {
    // hide all properties except Visible
    if (propertyName.equals(PROPERTY_NAME_WIDTH) ||
        propertyName.equals(PROPERTY_NAME_HEIGHT) ||
        propertyName.equals(H_A) ||
        propertyName.equals(V_A)) {
      return false;
    }

    return super.isPropertyVisible(propertyName);
  }

  @Override
  public boolean acceptableTarget() {
    return false;
  }

  @Override
  public String getVisibleTypeName() {
    return name;
  }

  @Override
  protected void onComponentAdded(MockComponent component) {
    super.onComponentAdded(component);
    int[] eventBits = new int[]{Event.ONMOUSEDOWN, Event.ONMOUSEUP,
        Event.ONMOUSEMOVE, Event.ONMOUSEOVER, Event.ONMOUSEOUT, Event.ONCLICK};
    for (int eventBit : eventBits) {
      component.unsinkEvents(eventBit);
    }
  }

  @Override
  public void onPropertyChange(String propertyName, String newValue) {

        OdeLog.log("in onPropertyChange");

    if (propertiesMap == null) {
      return;
    }

    JSONValue propJson = propertiesMap.get(propertyName);
    if (propJson != null) {
      JSONObject realPropInfo = propJson.asObject();
      String realCompName = realPropInfo.get("$Name").asString().getString();
      String realCompProp = realPropInfo.get("property").asString().getString();
      MockComponent realComp = findCompByInstanceName(this, realCompName);
      realComp.onPropertyChange(realCompProp, newValue);
    } else {
      super.onPropertyChange(propertyName, newValue);
    }
  }

  // find the component down the tree by the instance name
  private static MockComponent findCompByInstanceName(MockComponent comp, String name) {
    if (comp.getName().equals(name)) {
      return comp;
    }

    for (MockComponent child : comp.getChildren()) {
      MockComponent result = findCompByInstanceName(child, name);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private void initPropertiesMap() {
    if (propertiesMap != null) {
      return;
    }

    Ode ode = Ode.getInstance();
    long projectId = ode.getCurrentYoungAndroidProjectId();
    String fileId = "assets/external_comps/" + type + "/mapping.txt";
    OdeAsyncCallback<ChecksumedLoadFile> callback =
        new OdeAsyncCallback<ChecksumedLoadFile>() {
          @Override
          public void onSuccess(ChecksumedLoadFile result) {
            String content;
            try {
              content = result.getContent();
            } catch (ChecksumedFileException e) {
              onFailure(e);
              return;
            }

            JSONObject map = new ClientJsonParser().parse(content).asObject();
            propertiesMap = map.get("properties").asObject();
          }
        };

    ode.getProjectService().load2(projectId, fileId, callback);
  }

  private void buildDescendentTree(JSONObject composition) {
    JSONObject props = composition.get("Properties").asObject();

    JSONValue hAValue = props.get(H_A);
    JSONValue vAValue = props.get(V_A);

    if (hAValue != null) {
      changeProperty(H_A, hAValue.asString().getString());
    }

    if (vAValue != null) {
      changeProperty(V_A, vAValue.asString().getString());
    }

    JSONArray components = props.get("$Components").asArray();
    for (JSONValue component : components.getElements()) {
      ((YaFormEditor) editor).createMockComponent(component.asObject(), this);
    }
  }
}
