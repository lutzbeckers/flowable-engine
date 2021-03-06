/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the process instance query resource.
 * 
 * @author Frederik Heremans
 */
public class ProcessInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying process instance based on variables. POST query/process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstancesWithVariables() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);

        // Process variables
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("variables", variableArray);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals with non existing value
        variableNode.removeAll();
        variableNode.put("value", "Azerty2");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode);
    }

    /**
     * Test querying process instance based on variables. POST query/process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstancesPagingAndSorting() throws Exception {
        Authentication.setAuthenticatedUserId("queryAndSortingTestUser");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("aOneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("bOneTaskProcess");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("cOneTaskProcess");

        // Create request node
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("order", "desc");
        requestNode.put("sort", "processDefinitionKey");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check order
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode dataNode = rootNode.get("data");
        assertEquals(3, dataNode.size());

        assertEquals(processInstance3.getId(), dataNode.get(0).get("id").asText());
        assertEquals(processInstance2.getId(), dataNode.get(1).get("id").asText());
        assertEquals(processInstance1.getId(), dataNode.get(2).get("id").asText());

        // Check paging size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 0);
        requestNode.put("size", 1);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertEquals(1, dataNode.size());

        // Check paging start and size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 1);
        requestNode.put("size", 1);
        requestNode.put("order", "desc");
        requestNode.put("sort", "processDefinitionKey");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertEquals(1, dataNode.size());
        JsonNode valueNode = dataNode.get(0);
        assertEquals(processInstance2.getId(), valueNode.get("id").asText());
        assertEquals("The One Task Process", valueNode.get("processDefinitionName").asText());
        assertEquals("One task process description", valueNode.get("processDefinitionDescription").asText());
        assertThat(valueNode.has("startTime")).as("has startTime").isTrue();
        assertThat(valueNode.get("startUserId").textValue()).as("startUserId").isEqualTo(processInstance2.getStartUserId());
    }

}
