;; Part of the IMDb List Analyzer program.;; 'Math Tools' file provides basic mathematic functions for list analytics:;;  sum, mean, dot-product, variance, stdev, correlation;;;; Esa Junttila 2013-06-29;;(ns imdb-list-analyzer.math-tools);(set! *warn-on-reflection* true)  ; enable only for testing(defn sum  "Compute the sum of numbers. Zero is returned if 'nums' is empty."  [nums]  {:post (number? %)}  (if (= (count nums) 0)    0    (reduce + nums)))(defn mean  "Compute the mean of numbers. NaN is returned if 'nums' is empty."  [nums]  {:post (number? %)}  (let [n (count nums)]    (if (= n 0)      Double/NaN      (/ (sum nums) n))))(defn dot-product  "Compute the dot product between two sequences.   Both 'values1' and 'values2' must have equal sizes."  [nums1 nums2]  {:pre [(= (count nums1) (count nums2))]   :post (number? %)}  (reduce + (map * nums1 nums2)))(defn variance  "Compute the (sample) variance of numbers."  [nums]  {:pre [(>= (count nums) 2)]   :post [(or (>= % 0.0) (Double/isNaN %))]}  (let [mus (repeat (count nums) (mean nums))        diff (map - nums mus)]    (/ (dot-product diff diff) (dec (count nums)))))(defn stdev  "Compute the (sample) standard deviation of numbers."  [nums]  {:pre [(>= (count nums) 2)]   :post [(or (>= % 0.0) (Double/isNaN %))]}  (Math/sqrt (variance nums))); deprecated;(defn- st-score;  "Compute the standard scores for a sample of numbers.;   That is, deviations from mean in stdev units (sigma).";  [nums];  (let [n (count nums);        mu (mean nums);        sigma (stdev nums)];    (map #(/ (- % mu) sigma) nums)));;(defn correlation;  "Compute the Pearson product-moment correlation coefficient between two;   sequences of numbers.";  [nums1, nums2];  {:pre [(= (count nums1) (count nums2)) (>= (count nums1) 2)];   :post [(or (and (<= % 1.0) (>= % -1.0)) (Double/isNaN %))]};  (let [n (count nums1);        corr (/ (dot-product (st-score nums1) (st-score nums2)) (dec n))];    (cond  ; account for a small numerical error;      (> corr +1.0) +1.0;      (< corr -1.0) -1.0;      :else corr)))(defn- mean-diff  "Difference of values from the sample mean."  [nums]  (let [mu (mean nums)]    (map #(- % mu) nums)))(defn correlation  "Compute the Pearson product-moment correlation coefficient between two   sequences of numbers."  [nums1, nums2]  {:pre [(= (count nums1) (count nums2)) (>= (count nums1) 2)]   :post [(or (and (<= % 1.0) (>= % -1.0)) (Double/isNaN %))]}  (let [diff1 (mean-diff nums1)        diff2 (mean-diff nums2)        coeff1 (Math/sqrt (dot-product diff1 diff1))        coeff2 (Math/sqrt (dot-product diff2 diff2))    corr (/          (dot-product diff1 diff2)          (* coeff1 coeff2))]    (cond  ; account for a small numerical error      (> corr +1.0) +1.0      (< corr -1.0) -1.0      :else corr)))