/*
 * Copyright (c) 2012 - 2015, Internet Corporation for Assigned Names and
 * Numbers (ICANN) and China Internet Network Information Center (CNNIC)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * * Neither the name of the ICANN, CNNIC nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL ICANN OR CNNIC BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.restfulwhois.rdap.bootstrap.handler;

import java.util.ArrayList;
import java.util.List;

import org.restfulwhois.rdap.bootstrap.bean.NetworkRedirect;
import org.restfulwhois.rdap.bootstrap.bean.Redirect;
import org.restfulwhois.rdap.core.common.support.QueryParam;
import org.restfulwhois.rdap.core.common.util.IpUtil;
import org.restfulwhois.rdap.core.common.util.IpUtil.IpVersion;
import org.restfulwhois.rdap.core.ip.queryparam.NetworkQueryParam;
import org.restfulwhois.rdap.filters.QueryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This abstract class is used to Update network registry.
 * 
 * @author jiashuo
 * 
 */
@Component
public abstract class NetworkRegistryHandler extends RegistryHandler {
    /**
     * query parser.
     */
    @Autowired
    private QueryParser queryParser;

    /**
     * CIDR separator.
     */
    private static final String CIDR_SEPARATOR = "/";

    @Override
    void saveRedirects(List<Redirect> redirects) {
        redirectService.saveNetworkRedirect(redirects);
    }

    @Override
    List<Redirect> generateRedirects(String key, List<String> registryUrls) {
        List<Redirect> redirects = new ArrayList<Redirect>();
        if (!removeEmptyUrlsAndValidate(registryUrls)) {
            logger.error("ignore this key/urls:{},{}. Urls are empty.", key,
                    registryUrls);
            return redirects;
        }
        NetworkRedirect networkRedirect = new NetworkRedirect(registryUrls);
        IpVersion ipVersion = IpUtil.getIpVersionOfNetwork(key);
        if (ipVersion.isNotValidIp()) {
            logger.error("ignore this key/urls:{},{}. Invalid network:{}", key,
                    registryUrls);
            return redirects;
        }
        try {
            QueryParam queryParam = queryParser.parseIpQueryParam(key);
            if (null == queryParam) {
                logger.error(
                        "ignore this key/urls:{},{}. generate networkQueryParam error:{}",
                        key, registryUrls);
                return redirects;
            }
            networkRedirect
                    .setNetworkQueryParam((NetworkQueryParam) queryParam);
            redirects.add(networkRedirect);
        } catch (Exception e) {
            logger.error("ignore this key/urls:{},{}. Invalid network:{}", key,
                    registryUrls);
        }
        return redirects;
    }

}
