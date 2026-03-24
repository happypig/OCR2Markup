package com.dila.dama.plugin.infrastructure.api;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CBRDResponseTest {

    @Test
    public void fromJson_successWithFoundUrls_parsesCorrectly() {
        String json = "{\"success\":true,\"found\":[\"https://cbetaonline.dila.edu.tw/T1514\"]}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResults()).isTrue();
        assertThat(response.getFound()).containsExactly("https://cbetaonline.dila.edu.tw/T1514");
        assertThat(response.getFirstUrl()).isEqualTo("https://cbetaonline.dila.edu.tw/T1514");
        assertThat(response.getError()).isNull();
    }

    @Test
    public void fromJson_successWithEmptyFound_parsesCorrectly() {
        String json = "{\"success\":true,\"found\":[]}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResults()).isFalse();
        assertThat(response.getFound()).isEmpty();
        assertThat(response.getFirstUrl()).isNull();
    }

    @Test
    public void fromJson_errorWithErrorField_parsesErrorCorrectly() {
        String json = "{\"success\":false,\"found\":[],\"error\":\"Invalid reference format\"}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.hasResults()).isFalse();
        assertThat(response.getError()).isEqualTo("Invalid reference format");
    }

    @Test
    public void fromJson_errorWithMsgField_parsesErrorCorrectly() {
        // Actual CBETA API returns error message in "msg" field
        String json = "{\"success\":false,\"msg\":\"冊號不存在\"}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.hasResults()).isFalse();
        assertThat(response.getError()).isEqualTo("冊號不存在");
    }

    @Test
    public void fromJson_errorFieldTakesPrecedenceOverMsg() {
        // If both fields exist, "error" takes precedence for backwards compatibility
        String json = "{\"success\":false,\"error\":\"Error field\",\"msg\":\"Msg field\"}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Error field");
    }

    @Test
    public void fromJson_multipleUrls_parsesAllUrls() {
        String json = "{\"success\":true,\"found\":[\"https://url1.com\",\"https://url2.com\",\"https://url3.com\"]}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResults()).isTrue();
        assertThat(response.getFound()).hasSize(3);
        assertThat(response.getFirstUrl()).isEqualTo("https://url1.com");
    }

    @Test
    public void fromJson_missingFoundArray_createsEmptyList() {
        String json = "{\"success\":true}";
        
        CBRDResponse response = CBRDResponse.fromJson(json);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResults()).isFalse();
        assertThat(response.getFound()).isEmpty();
    }
}
