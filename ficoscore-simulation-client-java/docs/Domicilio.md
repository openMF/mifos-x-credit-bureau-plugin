

# Domicilio

Datos del domicilio de la persona a consultar

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**direccion** | **String** | Reportar el nombre de la calle, número exterior, número interior. Deben considerarse avenida,cerrada, manzana, lote, edificio,departamento etc. Debe contener por lo menos dos cadenas de caracteres para que el registro sea válido, de lo contrario el registro será rechazado. |  |
|**coloniaPoblacion** | **String** | Reportar la colonia a la cual pertenece la dirección contenida en el elemento dirección. |  |
|**delegacionMunicipio** | **String** | Reportar la delegación o municipio a la cual pertenece la dirección contenida en el elemento Dirección. |  |
|**ciudad** | **String** | Reportar la ciudad a la cual pertenece la dirección contenida en el elemento Dirección. |  |
|**estado** | **CatalogoEstados** |  |  |
|**CP** | **String** | El código postal reportado debe estar compuesto por 5 dígitos. Para que el código postal sea válido deberá corresponder al estado reportado. |  |
|**fechaResidencia** | **String** |  |  [optional] |
|**numeroTelefono** | **String** |  |  [optional] |
|**tipoDomicilio** | **CatalogoTipoDomicilio** |  |  [optional] |
|**tipoAsentamiento** | **CatalogoTipoAsentamiento** |  |  [optional] |
|**fechaRegistroDomicilio** | **String** | Fecha en la cual se registró el domicilio. |  [optional] |
|**tipoAltaDomicilio** | **BigDecimal** | Los valores posibles: 0 &#x3D; Alta de crédito 1 &#x3D; Consulta expediente |  [optional] |
|**idDomicilio** | **String** | Identificador único del domicilio |  [optional] |



