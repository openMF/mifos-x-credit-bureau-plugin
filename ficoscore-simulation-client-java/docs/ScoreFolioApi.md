# ScoreFolioApi

All URIs are relative to *https://services.circulodecredito.com.mx/v2/ficoscore*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**folio**](ScoreFolioApi.md#folio) | **POST** /folio |  |


<a id="folio"></a>
# **folio**
> ResponseScore folio(xSignature, xApiKey, username, password, scoreFolio)





### Example
```java
// Import classes:
import org.mifos.creditbureau.connector.circulodecredito.ApiClient;
import org.mifos.creditbureau.connector.circulodecredito.ApiException;
import org.mifos.creditbureau.connector.circulodecredito.Configuration;
import org.mifos.creditbureau.connector.circulodecredito.models.*;
import org.mifos.creditbureau.connector.circulodecredito.api.ScoreFolioApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://services.circulodecredito.com.mx/v2/ficoscore");

    ScoreFolioApi apiInstance = new ScoreFolioApi(defaultClient);
    String xSignature = "xSignature_example"; // String | Firma generada con la llave privada
    String xApiKey = "xApiKey_example"; // String | ConsumerKey obtenido desde el portal de desarrolladores
    String username = "username_example"; // String | Usuario de Círculo de Crédito
    String password = "password_example"; // String | Contraseña de Círculo de Crédito
    RequestScoreFolio scoreFolio = new RequestScoreFolio(); // RequestScoreFolio | 
    try {
      ResponseScore result = apiInstance.folio(xSignature, xApiKey, username, password, scoreFolio);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScoreFolioApi#folio");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xSignature** | **String**| Firma generada con la llave privada | |
| **xApiKey** | **String**| ConsumerKey obtenido desde el portal de desarrolladores | |
| **username** | **String**| Usuario de Círculo de Crédito | |
| **password** | **String**| Contraseña de Círculo de Crédito | |
| **scoreFolio** | [**RequestScoreFolio**](RequestScoreFolio.md)|  | |

### Return type

[**ResponseScore**](ResponseScore.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |
| **204** | NO CONTENT |  -  |
| **400** | BAD REQUEST |  -  |
| **401** | Unauthorized |  -  |
| **403** | FORBIDDEN |  -  |
| **404** | NOT FOUND |  -  |
| **405** | METHOD NOT ALLOWED |  -  |
| **415** | UNSUPPORTED MEDIA TYPE. |  -  |
| **429** | TOO MANY REQUESTS |  -  |
| **500** | INTERNAL SERVER ERROR |  -  |
| **503** | SERVICE UNAVAILABLE |  -  |

