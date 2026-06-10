package com.bank.customer.service;

import com.bank.customer.dto.CreateCustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.UpdateCustomerRequest;
import com.bank.customer.entity.Customer;
import com.bank.customer.entity.KycStatus;
import com.bank.customer.exception.ApiException;
import com.bank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

    /** Create the profile for the currently logged-in user (one per user). */
    public CustomerResponse createProfile(String userId, String email, CreateCustomerRequest request) {
        if (repository.existsByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Profile already exists for this user");
        }
        Customer customer = Customer.builder()
                .userId(userId)
                .email(email)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .address(request.address())
                .dateOfBirth(request.dateOfBirth())
                .kycStatus(KycStatus.PENDING)
                .build();
        return CustomerResponse.from(repository.save(customer));
    }

    public CustomerResponse getByUserId(String userId) {
        return CustomerResponse.from(findOrThrow(userId));
    }

    /** Partial update — only non-null fields are applied. */
    public CustomerResponse updateProfile(String userId, UpdateCustomerRequest request) {
        Customer customer = findOrThrow(userId);
        if (request.firstName() != null)   customer.setFirstName(request.firstName());
        if (request.lastName() != null)    customer.setLastName(request.lastName());
        if (request.phone() != null)       customer.setPhone(request.phone());
        if (request.address() != null)     customer.setAddress(request.address());
        if (request.dateOfBirth() != null) customer.setDateOfBirth(request.dateOfBirth());
        return CustomerResponse.from(repository.save(customer));
    }

    public List<CustomerResponse> getAll() {
        return repository.findAll().stream().map(CustomerResponse::from).toList();
    }

    /** Staff-only action: move a customer's KYC to VERIFIED / REJECTED / PENDING. */
    public CustomerResponse updateKyc(String userId, KycStatus status) {
        Customer customer = findOrThrow(userId);
        customer.setKycStatus(status);
        return CustomerResponse.from(repository.save(customer));
    }

    private Customer findOrThrow(String userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }
}
