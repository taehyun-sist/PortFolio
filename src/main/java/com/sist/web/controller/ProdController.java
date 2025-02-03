package com.sist.web.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.sist.common.model.FileData;
import com.sist.common.util.StringUtil;
import com.sist.web.model.Cart;
import com.sist.web.model.Order;
import com.sist.web.model.Paging;
import com.sist.web.model.Prod;
import com.sist.web.model.Response;
import com.sist.web.model.Review;
import com.sist.web.model.ReviewImg;
import com.sist.web.model.ShippingAddr;
import com.sist.web.model.StyleWishList;
import com.sist.web.model.User;
import com.sist.web.model.VariantsOptionValue;
import com.sist.web.model.WishList;
import com.sist.web.service.CartService;
import com.sist.web.service.OrderService;
import com.sist.web.service.ProdService;
import com.sist.web.service.ReviewService;
import com.sist.web.service.StyleWishService;
import com.sist.web.service.UserService;
import com.sist.web.service.WishListService;
import com.sist.web.util.CookieUtil;
import com.sist.web.util.HttpUtil;

@Controller("prodController")
public class ProdController 
{
   public static Logger logger = LoggerFactory.getLogger(ProdController.class);
   
   @Autowired
   private StyleWishService styleWishService;
   
   @Autowired
   private ProdService prodService;
   
   @Autowired
   private WishListService wishListService;
   
   @Autowired
   private CartService cartService;
   
   @Autowired
   private ReviewService reviewService;
   
   @Autowired
   private UserService userService;
   
   @Autowired
   private OrderService orderService;
   
   @Autowired
   JavaMailSenderImpl mailSender;
   
   @Value("#{env['auth.cookie.name']}")
   private String AUTH_COOKIE_NAME;
   
   @Value("#{env['x.naver.client.id']}")
   private String X_NAVER_CLIENT_ID;
   
   @Value("#{env['x.naver.client.secret']}")
   private String X_NAVER_CLIENT_SECRET;
   
   @Value("#{env['x.naverpay.chain.id']}")
   private String X_NAVERPAY_CHAIN_ID;
   
   @Value("#{env['tosspay.client.key']}")
   private String TOSSPAY_CLIENT_KEY;
   
   @Value("#{env['tosspay.secret.key']}")
   private String TOSSPAY_SECRET_KEY;
   
   @Value("#{env['review.img.dir']}")
   private String REVIEW_IMG_DIR;
   
   private static final int LIST_COUNT = 20;
   private static final int PAGE_COUNT = 5;
   
   @RequestMapping(value = "/shop/shopIndex", method=RequestMethod.GET)
   public String shopIndex(HttpServletRequest request, HttpServletResponse response, ModelMap model)
   {
	   String userEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
	   
	   if(StringUtil.isEmpty(userEmail))
	   {
		   userEmail = "";
	   }   
	   
	   List<Prod> list = prodService.popularityProd(userEmail);
	   
	   model.addAttribute("list", list);
	   
      return "/shop/shopIndex";
   }
   
   @RequestMapping(value = "/shop/shopList")
   public String shopList(HttpServletRequest request, HttpServletResponse response, ModelMap model)
   {
      long curPage = HttpUtil.get(request, "curPage", (long)1);
      String searchValue = HttpUtil.get(request, "searchValue", "");   //상품명, 브랜드
      String gender = HttpUtil.get(request, "gender", "all");
      String price = HttpUtil.get(request, "price", "all");
      String brand = HttpUtil.get(request, "brand", "all");
      String material = HttpUtil.get(request, "material", "all");
      String cateId = HttpUtil.get(request, "cateId", "");
      String array = HttpUtil.get(request, "array", "1");
      String userEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      
      logger.debug("cateId : ", cateId);
      logger.debug("gender : ", gender);
      logger.debug("brand : ", brand);
      
      long totalCount = 0;
       
      List<Prod> list = null;
      Prod prod = new Prod();
      Paging paging = null;
      
      if(!StringUtil.isEmpty(searchValue))
      {
         if(StringUtil.equals(searchValue, "나이키") || StringUtil.equals(searchValue, "nike"))
         {
            searchValue = "NIKE";
         }
         
         else if(StringUtil.equals(searchValue, "아디다스") || StringUtil.equals(searchValue, "addidas"))
         {
            searchValue = "ADDIDAS";
         }   
         
         else if(StringUtil.equals(searchValue, "푸마") || StringUtil.equals(searchValue, "puma"))
         {
            searchValue = "PUMA";
         }
         
         else if(StringUtil.equals(searchValue, "온") || StringUtil.equals(searchValue, "on"))
         {
            searchValue = "ON";
         }
         
         else if(StringUtil.equals(searchValue, "호카") || StringUtil.equals(searchValue, "hoka"))
         {
            searchValue = "HOKA";
         }
         
         prod.setSearchValue(searchValue);
      }
      
      
      prod.setArray(array);
      
      if(!StringUtil.isEmpty(userEmail))
      {
         prod.setUserEmail(userEmail);
      }
      
      
      if(!StringUtil.isEmpty(cateId))
      {
         prod.setCateId(cateId);
      }
      
      if(!StringUtil.isEmpty(gender) && !StringUtil.equals(gender, "all"))
      {
         prod.setGender(gender);
      }
      
      if(!StringUtil.isEmpty(brand) && !StringUtil.equals(brand, "all"))
      {
         prod.setBrand(brand);
      }
      
      if(!StringUtil.isEmpty(material) && !StringUtil.equals(material, "all"))
      {
         prod.setMaterial(material);
      }
      
      if(!StringUtil.isEmpty(price) && !StringUtil.equals(price, "all"))
      {
         if(StringUtil.equals(price, "100000"))
         {
            prod.setPriceStartRow(0);
            prod.setPriceEndRow(100000);
         }
         
         else if(StringUtil.equals(price, "200000"))
         {
            prod.setPriceStartRow(100000);
            prod.setPriceEndRow(200000);
         }
         
         else if(StringUtil.equals(price, "300000"))
         {
            prod.setPriceStartRow(200000);
            prod.setPriceEndRow(300000);
         }
         
         else if(StringUtil.equals(price, "400000"))
         {
            prod.setPriceStartRow(300000);
            prod.setPriceEndRow(400000);
         }
         
         else if(StringUtil.equals(price, "500000"))
         {
            prod.setPriceStartRow(400000);
            prod.setPriceEndRow(500000);
         }
         
         else if(StringUtil.equals(price, "600000"))
         {
            prod.setPriceStartRow(500000);
         }
      }
      
      totalCount = prodService.prodListCount(prod);
      
      if(totalCount > 0)
      {
         paging = new Paging("/shop/shopList", totalCount, LIST_COUNT, PAGE_COUNT, curPage, "curPage");
         prod.setStartRow(paging.getStartRow());
         prod.setEndRow(paging.getEndRow());
         
         list = prodService.prodList(prod);
      }
      
      model.addAttribute("list", list);
      model.addAttribute("searchValue", searchValue);
      model.addAttribute("curPage", curPage);
      model.addAttribute("paging", paging);
      model.addAttribute("cateId", cateId);
      model.addAttribute("gender", gender);
      model.addAttribute("price", price);
      model.addAttribute("brand", brand);
      model.addAttribute("array", array);
      model.addAttribute("material", material);
      
      return "/shop/shopList";
   }
   
   @RequestMapping(value = "/shop/detail")
   public String detail(HttpServletRequest request, HttpServletResponse response, ModelMap model)
   {   
      String prodId = HttpUtil.get(request, "prodId", "");
      String searchValue = HttpUtil.get(request, "searchValue");
      long curPage = HttpUtil.get(request, "curPage", (long)1 );
      String cateId = HttpUtil.get(request, "cateId", "");
      String userEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
            
      String cookieUserId = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);//
      User user = userService.userSelect(cookieUserId);//
      
      Prod prod = new Prod();
      
      List<VariantsOptionValue> optionCntList = null;
      
      List<VariantsOptionValue> optionList = null;
      
      List<VariantsOptionValue> sizeList = null;
      
      List<VariantsOptionValue> colorList = null;
      
      List<Review> reviewList = null;
      
      long reviewCount = 0;
      
      long ratingAvg = 0;
      
      int fullStars = 0;
      int emptyStars = 0;
      boolean hasHalfStar = false;
            
      
      if(!StringUtil.isEmpty(prodId))
      {
         optionCntList = prodService.optionCntList(prodId);
         
         if(optionCntList.size() > 1)
         {
            sizeList = prodService.sizeListView(prodId);
            colorList = prodService.colorListView(prodId);
            
            model.addAttribute("colorList", colorList);
            model.addAttribute("sizeList", sizeList);
         }
         
         else 
         {
            optionList = prodService.optionListView(prodId);
            model.addAttribute("optionList", optionList);
         }
         
         prod.setUserEmail(userEmail);
         prod.setProdId(prodId);
         
         prod = prodService.detailProdView(prod);
         reviewList = reviewService.reviewListSelect(prodId);
         ratingAvg = reviewService.ratingAvg(prodId);
         reviewCount = reviewService.reviewCount(prodId);
         fullStars = (int) Math.floor(ratingAvg);
         emptyStars = 5 - (int) Math.ceil(ratingAvg);
         hasHalfStar = (ratingAvg % 1) >= 0.5;      

      }
      
      model.addAttribute("user", user);//
      
      model.addAttribute("fullStars", fullStars);
      model.addAttribute("emptyStars", emptyStars);
      model.addAttribute("hasHalfStar", hasHalfStar);
      model.addAttribute("reviewCount", reviewCount);
      model.addAttribute("reviewList", reviewList);
      model.addAttribute("prodId", prodId);
      model.addAttribute("userEmail", userEmail);
      model.addAttribute("prod", prod);
      model.addAttribute("searchValue", searchValue);
      model.addAttribute("curPage", curPage);
      model.addAttribute("cateId", cateId);
      
      return "/shop/detail";
   }
   
   @RequestMapping(value = "/shop/sendEmail")
   @ResponseBody
   public Response<Object> sendEmail(HttpServletRequest request, HttpServletResponse response) 
   {
       Response<Object> res = new Response<Object>();

       // 입력 값 가져오기
       String userEmail = HttpUtil.get(request, "userEmail");
       String userName = HttpUtil.get(request, "userName");
       String prodName = HttpUtil.get(request, "prodName");
       String userMessage = HttpUtil.get(request, "message");
       
       logger.debug("userEmail : ", userEmail);

       // 이메일 양식 설정
       String setFrom = "pth1728@naver.com";
       String toMail = userEmail;
       String title = "[상품 문의] 관련 답변 이메일입니다.";
       String content = 
    		    "<div style=\"font-family: 'Arial', sans-serif; line-height: 1.8; color: #444; background-color: #f7f7f7; padding: 20px; border: 1px solid #ddd; border-radius: 8px;\">" +
    		    "    <h2 style=\"color: #333; margin-bottom: 20px;\">📩 새로운 상품 문의 알림</h2>" +
    		    "    <p>안녕하세요, 관리자님.</p>" +
    		    "    <p>새로운 상품 문의가 접수되었습니다. 아래 내용을 확인해주세요:</p>" +
    		    "    <div style=\"background-color: #fff; padding: 20px; border: 1px solid #ccc; border-radius: 6px; margin: 20px 0;\">" +
    		    "        <p><strong>문의자 이름:</strong> " + userName + "</p>" +
    		    "        <p><strong>문의 상품:</strong> " + prodName + "</p>" +
    		    "        <p><strong>문의자 이메일:</strong> <a href=\"mailto:" + userEmail + "\" style=\"color: #1a73e8; text-decoration: none;\">" + userEmail + "</a></p>" +
    		    "        <p><strong>문의 내용:</strong></p>" +
    		    "        <p style=\"background: #f9f9f9; padding: 15px; border: 1px solid #eee; border-radius: 4px;\">" + userMessage + "</p>" +
    		    "    </div>" +
    		    "    <p>확인 후 빠른 답변 부탁드립니다. 😊</p>" +
    		    "    <hr style=\"border: 0; border-top: 1px solid #ccc; margin: 20px 0;\">" +
    		    "    <p style=\"color: #999; font-size: 12px; text-align: center;\">본 이메일은 자동 발신 시스템에서 발송되었습니다. 답장하지 마세요.</p>" +
    		    "</div>";

       try 
       {
           // 이메일 전송 설정
           MimeMessage message = mailSender.createMimeMessage();
           MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
           helper.setFrom(setFrom);
           helper.setTo(toMail);
           helper.setSubject(title);
           helper.setText(content, true);
           mailSender.send(message);

           res.setResponse(0, "success");
       } 
       
       catch (Exception e) 
       {
           logger.error("[ProdController] sendEmail Exception", e);
           res.setResponse(-1, "fail");
       }

       return res;
   }

   @RequestMapping(value = "/shop/reviewAdd", method = RequestMethod.POST)
   @ResponseBody
   public Response<Object> reviewAdd(MultipartHttpServletRequest request, HttpServletResponse response)
   {
      Response<Object> res = new Response<Object>();
      
      String userCookieId = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      String userEmail = HttpUtil.get(request, "userReviewEmail");
      String prodId = HttpUtil.get(request, "prodId", "");
      String reviewContent = HttpUtil.get(request, "reviewContent", "");
      int reviewRating = HttpUtil.get(request, "reviewRating", 0);
      
      long reviewSeqSelect = reviewService.getReviewSeq();
      
      List<FileData> fileData = HttpUtil.getFiles(request, "reviewFiles", REVIEW_IMG_DIR, reviewSeqSelect);
      List<ReviewImg> reviewImgList = null;
      
      Review review = new Review();
      
      if(!StringUtil.isEmpty(prodId) && !StringUtil.isEmpty(userCookieId))
      {
         if(StringUtil.equals(userEmail, userCookieId))
         {
            if(!StringUtil.isEmpty(reviewContent))
            {
               review.setProdId(prodId);
               review.setUserEmail(userCookieId);
               review.setReviewContent(reviewContent);
               review.setReviewRating(reviewRating);
               review.setReviewStatus("Y");
               
               if(fileData != null)
               {
                  reviewImgList = new ArrayList<ReviewImg>();
                  
                  for(int i = 0; i < fileData.size(); i++)
                  {
                     if(fileData.get(i).getFileSize() > 0) 
                     {
                        ReviewImg reviewImg = new ReviewImg();

                        reviewImg.setUserEmail(userEmail);
                           reviewImg.setReviewImgName(fileData.get(i).getFileName());
                           reviewImg.setReviewImgOrgName(fileData.get(i).getFileOrgName());
                           reviewImg.setReviewImgExt(fileData.get(i).getFileExt());
                           reviewImg.setReviewImgSize(fileData.get(i).getFileSize());

                           reviewImgList.add(reviewImg);
                     }
                  }
               }
               
               review.setReviewImg(reviewImgList);
               
               try 
               {
                  if(reviewService.reviewInsert(review) > 0)
                  {
                     res.setResponse(0, "success");
                  }
                  
                  else 
                  {
                     res.setResponse(500, "fail");
                  }
               }
               
               catch (Exception e) 
               {
                  logger.error("[ProdController] reviewAdd Exception", e);
               }
            }
            
            else 
            {
               res.setResponse(400, "fail");
            }
         }
         
         else 
         {
            res.setResponse(404, "fail");
         }
      }
      
      else 
      {
         res.setResponse(405, "fail");
      }
      
      return res;
   }
   
   @RequestMapping(value = "/shop/reviewDel", method=RequestMethod.POST)
   @ResponseBody
   public Response<Object> reviewDel(HttpServletRequest request, HttpServletResponse response)
   {
      Response<Object> res = new Response<Object>();
      
      String cookieUserId = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      long reviewSeq = HttpUtil.get(request, "reviewSeq", 1L);
      String prodId = HttpUtil.get(request, "prodId", "");
      
      Review review = null;
      
      if(!StringUtil.isEmpty(prodId) && !StringUtil.isEmpty(cookieUserId))
      {
         User user = userService.userSelect(cookieUserId);
         
         review = reviewService.reviewSelect(reviewSeq);
         
         if(review != null)
         {
            if(StringUtil.equals(cookieUserId, user.getUserEmail()))
            {
               if(reviewService.reviewDelete(review) > 0)
               {
                  res.setResponse(0, "success");
               }
               
               else 
               {
                  res.setResponse(500, "서버 오류");
               }
            }
            
            else 
            {
               res.setResponse(400, "올바르지 않은 사용자");
            }
         }
         
         else 
         {
            res.setResponse(404, "해당 리뷰 글이 존재하지 않습니다.");
         }
      }
      
      else 
      {
         res.setResponse(405, "잘못된 접근");
      }
      
      return res;
   }
   
   @RequestMapping(value = "/shop/reviewSel", method = RequestMethod.POST)
   @ResponseBody
   public Response<Object> reviewSel(HttpServletRequest request, HttpServletResponse response)
   {
      Response<Object> res = new Response<Object>();
      
      long reviewSeq = HttpUtil.get(request, "reviewSeq", 1L);
      String cookieUserId = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      
      Review review = null;
      
      if(reviewSeq > 0 && !StringUtil.isEmpty(cookieUserId))
      {
         review = reviewService.reviewSelect(reviewSeq);
         
         if(review != null)
         {
            JsonObject json = new JsonObject();
            
            json.addProperty("reviewContent", review.getReviewContent());
            json.addProperty("userEmail", review.getUserEmail());
            json.addProperty("reviewRating", review.getReviewRating());
            json.addProperty("reviewSeq", review.getReviewSeq());
            
            res.setResponse(0, "success", json);
         }
         
         else 
         {
            res.setResponse(404, "리뷰가 존재하지 않습니다.");
         }
      }
      
      else 
      {
         res.setResponse(500, "잘못된 접근입니다.");
      }
      
      return res;
   }
   
   @RequestMapping(value = "/shop/reviewUpdate", method = RequestMethod.POST)
   @ResponseBody
   public Response<Object> reviewUpdate(MultipartHttpServletRequest request, HttpServletResponse response)
   {
      Response<Object> res = new Response<Object>();
      
      String userCookieId = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      String userEmail = HttpUtil.get(request, "userReviewEmail");
      String prodId = HttpUtil.get(request, "prodId", "");
      String reviewContent = HttpUtil.get(request, "reviewContent", "");
      int reviewRating = HttpUtil.get(request, "reviewRating", 0);
      
      long reviewSeq = HttpUtil.get(request, "reviewSeq", 1L);
      
      List<FileData> fileData = HttpUtil.getFiles(request, "reviewFilesUpdate", REVIEW_IMG_DIR, reviewSeq);
      List<ReviewImg> reviewImgList = null;
      
      Review review = new Review();
      
      if(!StringUtil.isEmpty(prodId) && !StringUtil.isEmpty(userCookieId))
      {
         User user = userService.userSelect(userEmail);
         
         if(StringUtil.equals(userCookieId, user.getUserEmail()))
         {
            if(!StringUtil.isEmpty(reviewContent))
            {
               review.setUserEmail(userEmail);
               review.setReviewContent(reviewContent);
               review.setReviewRating(reviewRating);
               review.setProdId(prodId);
               review.setReviewSeq(reviewSeq);
               
               if(fileData != null  && !fileData.isEmpty() )//
               {
                  reviewImgList = new ArrayList<ReviewImg>();
                  
                  for(int i = 0; i < fileData.size(); i++)
                  {
                     if(fileData.get(i).getFileSize() > 0) 
                     {
                        ReviewImg reviewImg = new ReviewImg();

                        reviewImg.setReviewSeq(reviewSeq);
                        reviewImg.setUserEmail(userEmail);
                           reviewImg.setReviewImgName(fileData.get(i).getFileName());
                           reviewImg.setReviewImgOrgName(fileData.get(i).getFileOrgName());
                           reviewImg.setReviewImgExt(fileData.get(i).getFileExt());
                           reviewImg.setReviewImgSize(fileData.get(i).getFileSize());

                           reviewImgList.add(reviewImg);
                     }
                  }
                 
                  review.setReviewImg(reviewImgList);
               }
               
               try 
               {
                  if(reviewService.reviewUpdate(review) > 0)
                  {
                     res.setResponse(0, "success");
                  }
                  
                  else 
                  {
                     res.setResponse(500, "서버 오류");
                  }
               }
               
               catch (Exception e) 
               {
                  logger.error("[ProdController] reviewUpdate Exception", e);
               }      
            }
            
            else 
            {
               res.setResponse(402, "파라미터 값이 비어 있습니다.");
            }
         }
         
         else 
         {
            res.setResponse(405, "올바른 사용자가 아닙니다.");
         }
      }
      
      else 
      {
         res.setResponse(400, "잘못된 접근입니다.");
      }
      
      return res;
   }
   
   
   @RequestMapping(value = "/shop/cart")
	public String cart(Model model, HttpServletRequest request) {
		String userEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
		
		Map<String, Object> hashMap = new HashMap<>();
		hashMap.put("userEmail", userEmail);
		
		List<Cart> cartList = cartService.cartList(hashMap);
		long totalProdPrice = cartService.getTotalProdPrice(hashMap);
		
		String userGrade = userService.userSelect(userEmail).getUserGrade();
		
		double orderDiscountRate = 0.0;
		
		switch(userGrade) {
			case "BRONZE": 
				orderDiscountRate = 1.0;
				break;
			
			case "SILBER": 
				orderDiscountRate = 1.5;
				break;
				
			case "GOLD":
				orderDiscountRate = 2.0;
				break;
				
			case "PLATINUM":
				orderDiscountRate = 3.0;
				break;
		}
		
        BigDecimal totalProdPriceBD = new BigDecimal(totalProdPrice);  // 제품 총 가격
        BigDecimal orderDiscountRateBD = new BigDecimal(orderDiscountRate).divide(new BigDecimal(100));  // 할인율
        BigDecimal totalDiscountPriceBD = totalProdPriceBD.multiply(orderDiscountRateBD) .setScale(0, RoundingMode.HALF_UP);  
        long totalDiscountPrice = totalDiscountPriceBD.longValue();

		model.addAttribute("cartList", cartList);
		model.addAttribute("totalProdPrice", totalProdPrice);
		model.addAttribute("totalDiscountPrice", totalDiscountPrice);
		model.addAttribute("orderDiscountRate", orderDiscountRate);
		
		return "/shop/cart";
	}
	
	@RequestMapping(value = "/shop/order")
	public String order(Model model, HttpServletRequest request) {
	    String prodIds = HttpUtil.get(request, "prodIds", "");
	    String combIds = HttpUtil.get(request, "combIds", "");
	    String userEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
	   
	    List<String> prodIdList = new ArrayList<>(Arrays.asList(prodIds.split(",", -1)));
	    List<String> variantsOptionValueCombIdList = new ArrayList<>(Arrays.asList(combIds.split(",", -1)));
	    
	    List<Map<String, Object>> prodCombList = new ArrayList<>();
	    for (int i = 0; i < prodIdList.size(); i++) {
	        Map<String, Object> item = new HashMap<>();
	        item.put("prodId", prodIdList.get(i));
	        item.put("variantsOptionValueCombId", variantsOptionValueCombIdList.get(i));
	        prodCombList.add(item);
	    }
	   
	    Map<String, Object> hashMap = new HashMap<>();
	    hashMap.put("userEmail", userEmail);
	    hashMap.put("prodCombList", prodCombList);
	    
	    List<Cart> cartList = cartService.cartList(hashMap);
	    long totalProdPrice = cartService.getTotalProdPrice(hashMap);
	    
		String userGrade = userService.userSelect(userEmail).getUserGrade();
		
		double orderDiscountRate = 0.0;
		
		switch(userGrade) {
			case "BRONZE": 
				orderDiscountRate = 1.0;
				break;
			
			case "SILBER": 
				orderDiscountRate = 1.5;
				break;
				
			case "GOLD":
				orderDiscountRate = 2.0;
				break;
				
			case "PLATINUM":
				orderDiscountRate = 3.0;
				break;
		}
	    
        BigDecimal totalProdPriceBD = new BigDecimal(totalProdPrice);  // 제품 총 가격
        BigDecimal orderDiscountRateBD = new BigDecimal(orderDiscountRate).divide(new BigDecimal(100));  // 할인율
        BigDecimal totalDiscountPriceBD = totalProdPriceBD.multiply(orderDiscountRateBD) .setScale(0, RoundingMode.HALF_UP);  
        long totalDiscountPrice = totalDiscountPriceBD.longValue();
        
	    int orderShippingCost = 5000;
	    
	    if (totalProdPrice - totalDiscountPrice > 100000) {
	    	orderShippingCost = 0;
	    } 
	    
	    long orderTotalPrice = totalProdPrice - totalDiscountPrice + orderShippingCost;
	    
	    ShippingAddr defaultAddr = userService.userDefaultAddrSelect(userEmail);
	    
	    if (defaultAddr != null) {
		    defaultAddr.setRecipientTel(defaultAddr.getRecipientTel().replaceAll("(01[0-9])(\\d{3,4})(\\d{4})", "$1-$2-$3")); 
	    }
	    
	    List<ShippingAddr> addrList = userService.userAddrList(userEmail);

	    ObjectMapper objectMapper = new ObjectMapper();
	    
	    try {
	        model.addAttribute("addrListJson", objectMapper.writeValueAsString(addrList));
	        model.addAttribute("cartListJson", objectMapper.writeValueAsString(cartList));
	    
	    } catch (Exception e) {
	        logger.error("[ProdController] order Exception", e);
	    }
	    
	    User user = userService.userSelect(userEmail);
	    
	    model.addAttribute("prodIds", prodIds);
	    model.addAttribute("combIds", combIds);
	    model.addAttribute("cartList", cartList);
	    model.addAttribute("totalProdPrice", totalProdPrice);
	    model.addAttribute("totalDiscountPrice", totalDiscountPrice);
	    model.addAttribute("orderTotalPrice", orderTotalPrice);
	    model.addAttribute("orderShippingCost", orderShippingCost);
	    model.addAttribute("defaultAddr", defaultAddr);
	    model.addAttribute("user", user);
	    model.addAttribute("X_NAVER_CLIENT_ID", X_NAVER_CLIENT_ID);
	    model.addAttribute("X_NAVER_CLIENT_SECRET", X_NAVER_CLIENT_SECRET);
	    model.addAttribute("X_NAVERPAY_CHAIN_ID", X_NAVERPAY_CHAIN_ID);
	    model.addAttribute("TOSSPAY_CLIENT_KEY", TOSSPAY_CLIENT_KEY);
	    model.addAttribute("customerKey", UUID.randomUUID().toString());
	    
	    return "/shop/order";
	}
	
	@RequestMapping(value = "/shop/payOk")
	public String payOk(Model model, HttpServletRequest request, @RequestParam("orderId") String orderId) {
		Order order = orderService.orderSelect(orderId);
		
		String userEamil = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
		User user = userService.userSelect(userEamil);
		
		model.addAttribute("order", order);
		model.addAttribute("user", user);

		return "/shop/payOk";
	}
   
   @RequestMapping(value = "/shop/contact", method=RequestMethod.GET)
   public String contact(HttpServletRequest request, HttpServletResponse response)
   {
      return "/shop/contact";
   }
   
   @RequestMapping(value = "/shop/wish", method=RequestMethod.GET)
   public String wish(HttpServletRequest request, HttpServletResponse response, ModelMap model)
   {   
      String cookieUserEmail = CookieUtil.getHexValue(request, AUTH_COOKIE_NAME);
      
      List<WishList> wishList = null;
      List<StyleWishList> styleWishList = null;
      
      Paging paging = null;
      
      int totalCount = 0;
      long styleTotalCount = 0;
      
      totalCount = wishListService.wishListCount(cookieUserEmail);
      styleTotalCount = styleWishService.stylewishListCount(cookieUserEmail);
      
      logger.debug("========================");
      logger.debug("styleTotalCount : " + styleTotalCount);
      logger.debug("========================");
      
      if(totalCount > 0)
      {
         wishList = wishListService.wishList(cookieUserEmail);
      }
      
      if(styleTotalCount > 0)
      {
         styleWishList = styleWishService.styleWishList(cookieUserEmail);
      }
      
      model.addAttribute("wishList", wishList);
      model.addAttribute("totalCount", totalCount);
      model.addAttribute("styleWishList", styleWishList);
      model.addAttribute("styleTotalCount", styleTotalCount);
      
      logger.debug("@@@@@@@@@@@@" + wishList);
      logger.debug("@@@@@@@@@@@@" + totalCount);
      logger.debug("@@@@@@@@@@@@" + styleWishList);
      logger.debug("@@@@@@@@@@@@" + styleTotalCount);
      logger.debug("@@@@@@@@@@@@" + cookieUserEmail);
   
      return "/shop/wish";
   }
}