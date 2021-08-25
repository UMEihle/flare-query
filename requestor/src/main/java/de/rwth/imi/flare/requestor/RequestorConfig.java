package de.rwth.imi.flare.requestor;


import java.net.Authenticator;
import java.net.URI;

public interface RequestorConfig {

    /**
     *
     * @return Authenticator that allows the Requestor to search for Resources on the server
     */
    Authenticator getAuthentication();

    /**
     *
     * @return URI pointing to the Base address of the FHIR Server, such that concatenating /Patient/1
     * would return the Patient with ID 1
     */
    URI getBaseURI();
}
