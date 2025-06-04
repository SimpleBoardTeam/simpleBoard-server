package com.simpleboard.board.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <h4>공통 컨트롤러</h4>
 *
 * <p>헬스체크용 엔드포인트 제공</p>
 */
@Controller
public class BaseController {

    /**
     * <h5>헬스 체크 API</h5>
     */
    @GetMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        return "OK";
    }
}
