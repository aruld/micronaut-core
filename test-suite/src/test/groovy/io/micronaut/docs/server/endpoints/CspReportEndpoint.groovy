/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.docs.server.endpoints

//tag::import[]
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.management.endpoint.annotation.Endpoint
import io.micronaut.management.endpoint.annotation.Write
//end::import[]

//tag::classBegin[]
@Endpoint(id = "report", defaultSensitive = false)
class CspReportEndpoint {
//end::classBegin[]

    //tag::type[]
    public static final String APPLICATION_CSP_REPORT = "application/csp-report" //<1>
    //end::type[]

    //tag::report[]
    @Write(consumes = [MediaType.APPLICATION_JSON, APPLICATION_CSP_REPORT]) //<2>
    HttpStatus report(@Body String violation) {
        // record violation
        return HttpStatus.OK
    }
    //end::report[]

//tag::classEnd[]
}
//end::classEnd[]