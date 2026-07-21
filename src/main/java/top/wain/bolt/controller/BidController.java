package top.wain.bolt.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import top.wain.bolt.context.BidScopedContext;
import top.wain.bolt.model.context.BidContext;
import top.wain.bolt.model.request.BidRequest;
import top.wain.bolt.model.response.BidResponse;
import top.wain.bolt.service.BidService;

import java.util.UUID;

/**
 * @Description: 竞价入口，接收 SSP/ADX 的竞价请求
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
@RestController
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/bid")
    public BidResponse bid(@RequestBody BidRequest request,
                           HttpServletRequest servletRequest) {
        BidContext ctx = new BidContext(
                request.id() != null ? request.id() : UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                getClientIp(servletRequest)
        );

        return ScopedValue.where(BidScopedContext.CURRENT, ctx)
                .call(() -> bidService.bid(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
