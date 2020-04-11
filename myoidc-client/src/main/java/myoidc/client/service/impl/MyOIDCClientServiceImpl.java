package myoidc.client.service.impl;

import com.google.common.collect.ImmutableMap;
import myoidc.client.domain.RPHolder;
import myoidc.client.domain.RPHolderRepository;
import myoidc.client.service.MyOIDCClientService;
import myoidc.client.service.dto.AccessTokenDto;
import myoidc.client.service.dto.AuthAccessTokenDto;
import myoidc.client.service.dto.AuthCallbackDto;
import myoidc.client.service.dto.UserInfoDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 2020/4/7
 *
 * @author Shengzhao Li
 * @since 1.1.0
 */
@Service
public class MyOIDCClientServiceImpl implements MyOIDCClientService {

    private static final Logger LOG = LoggerFactory.getLogger(MyOIDCClientServiceImpl.class);

    @Autowired
    private RPHolderRepository rpHolderRepository;


    @Autowired
    private RestTemplate restTemplate;


    @Override
    public RPHolder loadRPHolder() {
        return rpHolderRepository.findRPHolder();
    }

    @Override
    public boolean saveRPHolder(RPHolder rpHolder) {
        boolean saveOK = rpHolderRepository.saveRPHolder(rpHolder);
        return saveOK && rpHolder.getDiscoveryEndpointInfo() != null;
    }

    @Override
    public AuthAccessTokenDto createAuthAccessTokenDto(AuthCallbackDto callbackDto) {
        RPHolder rpHolder = loadRPHolder();
        return new AuthAccessTokenDto()
                .setAccessTokenUri(rpHolder.getDiscoveryEndpointInfo().getToken_endpoint())
                .setCode(callbackDto.getCode());
    }

    @Override
    public AccessTokenDto retrieveAccessTokenDto(AuthAccessTokenDto tokenDto) {
        final String fullUri = tokenDto.getAccessTokenUri();
        LOG.debug("Get access_token URL: {}", fullUri);

        return loadAccessTokenDto(fullUri, tokenDto.getAuthCodeParams());
    }

    @Override
    public UserInfoDto loadUserInfoDto(String accessToken) {
        LOG.debug("Load OIDC User_Info by access_token = {}", accessToken);

        if (StringUtils.isBlank(accessToken)) {
            return new UserInfoDto("Illegal 'access_token'", "'access_token' is empty");
        } else {
            RPHolder rpHolder = loadRPHolder();
            try {
                String userinfoEndpoint = rpHolder.getDiscoveryEndpointInfo().getUserinfo_endpoint();
                return restTemplate.getForObject(userinfoEndpoint, UserInfoDto.class, ImmutableMap.of("access_token", accessToken));
            } catch (RestClientException e) {
                LOG.error("Rest error", e);
            }
            return null;
        }
    }


    private AccessTokenDto loadAccessTokenDto(String fullUri, Map<String, String> params) {
        try {
            return restTemplate.postForObject(fullUri, params, AccessTokenDto.class);
        } catch (RestClientException e) {
            LOG.error("Send request to: {} error", fullUri, e);
        }
        return null;
    }
}
