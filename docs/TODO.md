# TODO List
This document outlines the tasks and 
features that need to be completed or improved in the project.

## Setup and Configuration Issues

## Security Issues
- [ ] AES encryption is not enough, consider AWS secrets manager

## Performance Issues
- [ ] When retrieving the keys of a credit bureau privately, currently need to know its ID number
- [ ] Add caching for credit bureau secrets 
- [ ] Add logging for performance monitoring
- [ ] Missing tests
- [ ] Inconsistency with spring and jarkarta usage


## Features
### Retrieving Consolidated Credit Report for Production

- currently connected to sandbox. so data from fineract client has not been used yet. request body is currently hard coded
- we do not know if it will work if the request does not have all the information required in the request.
- [ ] implement mapper for fineract client data to rcc dto



