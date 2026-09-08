package com.hogwai.rowtransformers.controller;

import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import com.hogwai.rowtransformers.entity.Book;
import com.hogwai.rowtransformers.service.BookService;
import jakarta.persistence.Tuple;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller demonstrating different RowTransformer selections.
 * Each endpoint triggers a different transformer via the service layer.
 */
@RestController
@RequestMapping("/api/transformers")
class RowTransformerController {

    private final BookService bookService;

    RowTransformerController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/singular")
    List<Book> singularReturn() {
        return bookService.findAll();
    }

    @GetMapping("/constructor")
    List<BookSummaryDTO> constructor() {
        return bookService.findSummaries();
    }

    @GetMapping("/tuple")
    List<Tuple> tuple() {
        return bookService.findTuples();
    }

    @GetMapping("/map")
    List<Map<String, Object>> map() {
        return bookService.findMaps();
    }

    @GetMapping("/array")
    List<Object[]> array() {
        return bookService.findArrays();
    }

    @GetMapping("/list")
    List<List<Object>> list() {
        return bookService.findLists();
    }

    @GetMapping("/custom")
    List<BookSummaryDTO> custom() {
        return bookService.findCustom();
    }

    @GetMapping("/native-sql")
    List<Book> nativeSql() {
        return bookService.findAllNative();
    }
}
