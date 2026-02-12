/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClusterApplication {

    @JsonProperty("appFile")
    private String appFile = null;

    @JsonProperty("packageManager")
    private String packageManager = null;

    @JsonProperty("appName")
    private String appName = null;

    @JsonProperty("action")
    private String action = null;

    @JsonProperty("flags")
    private String flags = null;
}

