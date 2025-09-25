Fineract Address System:

1. Add Address Type
2. Add State
3. Add Country
4. Add Address
```
{
    "addressType": 15,
    "addressLine1": "Vina del Mar 33",
    "addressLine2": "Zacatenco",
    "addressLine3": "",
    "city": "CDMX",
    "stateProvinceId": 16,
    "countryId": 17,
    "postalCode": "07360"
}
```

Mexican Address Format:
1. Street name and number (Calle + número exterior, sometimes also número interior if it’s an apartment or suite). 
2. Colonia (neighborhood, subdivision, or development). 
3. Delegación/Municipio (borough in Mexico City, or municipality elsewhere). 
4. Código Postal (CP) (five-digit postal code). 
5. Ciudad/Localidad (city or town). 
6. Estado (state). 
7. País (country).

CirculoDeCreditoRcc Request Json:

```
{
  "primerNombre": "JUAN",
  "apellidoPaterno": "PRUEBA",
  "apellidoMaterno": "CUATRO",
  "fechaNacimiento": "1980-01-04",
  "rfc": "PUAC800107",
  "domicilio": {
    "direccion": "INSURGENTES SUR 1007",
    "colonia": "INSURGENTES SUR",
    "municipio": "CIUDAD DE MEXICO",
    "ciudad": "CIUDAD DE MEXICO",
    "estado": "DF",
    "codigoPostal": "11230"
  }
}
```

Assumptions made through client to circulo do credito mapper.
- first address line is street
- second address line is colonio
- municipio is ?? address line 3
- ciudad is city
- postal code is codigo postal
- state province is estado
- 