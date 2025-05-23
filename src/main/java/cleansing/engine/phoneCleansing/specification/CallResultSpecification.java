package cleansing.engine.phoneCleansing.specification;

import cleansing.engine.phoneCleansing.model.CallResult;
import org.springframework.data.jpa.domain.Specification;

public class CallResultSpecification {
    public static Specification<CallResult> hasKeyword(String keyword) {
        return (root, query, builder) ->
                keyword == null ? null : builder.like(root.get("phoneNumber"), "%" + keyword + "%");
    }

    public static Specification<CallResult> hasStatus(String status) {
        return (root, query, builder) ->
                status == null ? null : builder.equal(root.get("resultCall"), status);
    }
}
