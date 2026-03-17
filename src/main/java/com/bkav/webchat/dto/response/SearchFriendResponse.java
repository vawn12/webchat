package com.bkav.webchat.dto.response;

import com.bkav.webchat.dto.m.FriendSearchDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchFriendResponse {
    private int status;
    private List<FriendSearchDTO> friends;
}
