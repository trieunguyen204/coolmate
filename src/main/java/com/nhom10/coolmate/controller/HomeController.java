package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.category.CategoryService;
import com.nhom10.coolmate.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;


    @GetMapping({"/", "/user/home","/user/"})
    public String home(Model model) {

        model.addAttribute("featuredProducts", productService.getFeaturedProducts());

        int cartItemCount = 0;

        model.addAttribute("cartItemCount", cartItemCount);

        return "/user/home";
    }


    @GetMapping("/user/about")
    public String about() {
        return "/user/about";
    }


    @GetMapping("/user/contact")
    public String contact() {
        return "/user/contact";
    }

    @GetMapping("/user/product")
    public String product() {
        return "/user/product";
    }

    @GetMapping("/products/by-category/{id}")
    public String productByCategory(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findByCategoryId(id));
        return "/user/product-list";
    }


}