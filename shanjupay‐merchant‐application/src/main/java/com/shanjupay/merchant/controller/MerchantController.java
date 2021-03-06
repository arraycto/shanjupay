package com.shanjupay.merchant.controller;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.PhoneUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.MerchantDetailVO;
import com.shanjupay.merchant.common.util.SecurityUtil;
import com.shanjupay.merchant.convert.MerchantDetailConvert;
import com.shanjupay.merchant.convert.MerchantRegisterConvert;
import com.shanjupay.merchant.service.FileService;
import com.shanjupay.merchant.service.SmsService;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @author : xsh
 * @create : 2020-08-15 - 23:53
 * @describe:
 */
@Api(value = "商户平台‐商户相关", tags = "商户平台‐商户相关", description = "商户平台‐商户相关")
@RestController
@Slf4j
public class MerchantController {

    @Reference//注入远程调用接口
    private MerchantService merchantService;
    @Autowired//注入本地bean
    private SmsService smsService;
    @Autowired
    private FileService fileService;

    @ApiOperation("根据id查询商户")
    @GetMapping("/merchants/{id}")
    public MerchantDTO queryMerchantById(@PathVariable("id") Long id) {
        MerchantDTO merchantDTO = merchantService.queryMerchantById(id);
        return merchantDTO;
    }

    @ApiOperation("测试")
    @GetMapping(path = "/hello")
    public String hello() {
        return "hello";
    }

    @ApiOperation("测试")
    @ApiImplicitParam(name = "name", value = "姓名", required = true, dataType = "string")
    @PostMapping(value = "/hi")
    public String hi(String name) {
        return "hi," + name;
    }

    @ApiOperation("获取登录用户的商户信息")
    @GetMapping(value = "/my/merchants")
    public MerchantDTO getMyMerchantInfo() {
        Long merchantId = SecurityUtil.getMerchantId();
        MerchantDTO merchant = merchantService.queryMerchantById(merchantId);
        return merchant;
    }

    @ApiOperation("获取手机验证码")
    @ApiImplicitParam(name = "phone", value = "手机号", required = true, dataType = "String", paramType = "query")
    @GetMapping("/sms")
    public String getSMSCode(@RequestParam String phone) {
        log.info("向手机号:{}发送验证码", phone);

        return smsService.sendMsg(phone);
    }


    @ApiOperation("注册商户")
    @ApiImplicitParam(name = "merchantRegister", value = "注册信息", required = true, dataType = "MerchantRegisterVO", paramType = "body")
    @PostMapping("/merchants/register")
    public MerchantRegisterVO registerMerchant(@RequestBody MerchantRegisterVO merchantRegister) {

        //校验参数的合法性
        if (merchantRegister == null) {
            throw new BusinessException(CommonErrorCode.E_100108);
        }
        if (StringUtils.isBlank(merchantRegister.getMobile())) {
            throw new BusinessException(CommonErrorCode.E_100112);
        }

        //手机号格式校验
        if (!PhoneUtil.isMatches(merchantRegister.getMobile())) {
            throw new BusinessException(CommonErrorCode.E_100109);
        }


        //校验验证码
        smsService.checkVerifiyCode(merchantRegister.getVerifiykey(), merchantRegister.getVerifiyCode());

        //注册商户，向DTO写入商户注册信息
//        MerchantDTO merchantDTO = new MerchantDTO();
//        merchantDTO.setUsername(merchantRegister.getUsername());
//        merchantDTO.setMobile(merchantRegister.getMobile());
//        merchantDTO.setPassword(merchantRegister.getPassword());

        /*使用MapStruct转换对象*/
        MerchantDTO merchantDTO = MerchantRegisterConvert.INSTANCE.vo2dto(merchantRegister);
        merchantService.createMerchant(merchantDTO);
        return merchantRegister;
    }

    //上传证件照
    @ApiOperation("上传证件照")
    @PostMapping("/upload")
    public String upload(@ApiParam(value = "证件照", required = true) @RequestParam("file") MultipartFile multipartFile) throws IOException {

        //调用fileService上传文件
        //生成的文件名称fileName，要保证它的唯一
        //文件原始名称
        String originalFilename = multipartFile.getOriginalFilename();
        //扩展名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") - 1);
        //文件名称
        String fileName = UUID.randomUUID() + suffix;
        //byte[] bytes,String fileName
        return fileService.upload(multipartFile.getBytes(), fileName);
    }

    @ApiOperation("商户资质申请")
    @ApiImplicitParams({@ApiImplicitParam(name = "merchantInfo", value = "商户认证资料", required = true, dataType = "MerchantDetailVO", paramType = "body")})
    @PostMapping("/my/merchants/save")
    public void saveMerchant(@RequestBody MerchantDetailVO merchantInfo) {
        //取出当前登录的商户id，解析token获取商户id
        Long merchantId = SecurityUtil.getMerchantId();
        System.out.println(merchantId);

        //将MerchantDetailVO转为merchantDTO
        MerchantDTO merchantDTO = MerchantDetailConvert.INSTANCE.vo2dto(merchantInfo);
        merchantService.applyMerchant(merchantId, merchantDTO);
    }

}
