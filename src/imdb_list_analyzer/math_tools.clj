;;; 'Math Tools' file provides basic mathematical functions for list analytics:;;;  sum, mean, dot-product, variance, stdev, correlation.;;;;;;  These functions strive for numerical stability at the expense of speed.;;;  Pre- and post-contracts are used extensively to guard against improper;;;  use of the functions.;;;;;; Esa Junttila 2015-11-01 (originally 2013-07-02);;;(ns imdb-list-analyzer.math-tools  (:require [imdb-list-analyzer.common :as com :refer :all]));(set! *warn-on-reflection* true)  ; enable only for testing(defn non-neg-num?  "Is the argument a non-negative number?"  [x]  (and (number? x) (<= 0 x)))(defn non-pos-num?  "Is the argument a non-positive number?"  [x]  (and (number? x) (>= 0 x)))(defn neg-num?  "Is the argument a negative number?"  [x]  (and (number? x) (neg? x)))(defn pos-num?  "Is the argument a positive number?"  [x]  (and (number? x) (pos? x)))(defn log-n  "Return the 'base' logarithm of x"  [x base]  {:pre [(non-neg-num? x) (pos-num? base)]   :post (number? %)}  (if (= (double base) 1.0)    Double/NaN    (/ (Math/log x) (Math/log base))))(defn log-2  "Return the 2-base logarithm of x"  [x]  {:pre [(non-neg-num? x)]   :post (number? %)}  (log-n x 2))(defn sum  "Compute the sum of a collection of numbers.   Return zero if the collection is empty."  [coll]  {:pre [(coll? coll) (every? number? coll)]   :post (number? %)}  (reduce + coll))(defn mean  "Compute the mean of a collection of numbers.   Return NaN if the collection is empty."  [coll]  {:pre [(coll? coll) (every? number? coll)]   :post (number? %)}  (if (empty? coll)    Double/NaN    (/ (sum coll) (count coll))))(defn dot-product  "Compute the dot product between two equal-size collections of numbers.   Return zero if the collections are empty."  [coll1 coll2]  {:pre [(coll? coll1)         (coll? coll2)         (= (count coll1) (count coll2))         (every? number? coll1)         (every? number? coll2)]   :post (number? %)}  (sum (map * coll1 coll2)))(defn variance  "Compute the sample variance from a collection of at least two numbers."  [coll]  {:pre [(coll? coll)         (>= (count coll) 2)         (every? number? coll)]   :post [(or (>= % 0.0) (Double/isNaN %))]}  (let [mus (repeat (count coll) (mean coll))        diff (map - coll mus)]    (/ (dot-product diff diff) (dec (count coll)))))(defn stdev  "Compute the sample standard deviation from a collection of at least two numbers."  [coll]  {:pre [(coll? coll)         (>= (count coll) 2)         (every? number? coll)]   :post [(or (>= % 0.0) (Double/isNaN %))]}  (Math/sqrt (variance coll)))(defn- std-z-score  "Compute the standard z-scores for a sample of numbers.   That is, deviations from mean in stdev units (sigma)."  [coll]  (let [n (count coll)        mu (mean coll)        sigma (stdev coll)]    (if (zero? sigma)      (repeat n Double/NaN)      (map #(/ (- % mu) sigma) coll))))(defn correlation  "Compute the Pearson product-moment correlation coefficient between two   collections of numbers. The size of collections must be at least 2."  [coll1 coll2]  {:pre [(coll? coll1)         (coll? coll2)         (= (count coll1) (count coll2))         (>= (count coll1) 2)         (every? number? coll1)         (every? number? coll2)]   :post [(or (<= -1.0 % +1.0) (Double/isNaN %))]}  (let [n (count coll1)        corr (/ (dot-product (std-z-score coll1) (std-z-score coll2)) (dec n))]    (cond  ; handle small numerical instabilities like '1.0000000000000002'      (> corr +1.0) +1.0      (< corr -1.0) -1.0      :else corr)))(defn entropy  "Compute Shannon's information-theoretic entropy from the probabilities  or empirical frequencies of a probability distribution. The result  is represented as bits (base 2 logarithms are used.)"  [freqs]  {:pre [(coll? freqs) (every? non-neg-num? freqs)]   :post (pos-num? %)}  (let [non-zero-freqs (filter pos? freqs)        total (sum non-zero-freqs)        pr (map #(/ % total) non-zero-freqs)        pr-log-2 (map #(log-2 %) pr)]    (if (or (empty? non-zero-freqs) (zero? total))      Double/NaN      (Math/abs ^double (- (dot-product pr pr-log-2))))))"Empirical discrete probability distribution:sorted cumulative probabilities (most probable first) and the empirical mean"(defrecord EmpiricalDistr [symbcumuprobs empirical-mean])(defn gen-emp-distr  "Create an empirical discrete probability distribution from a sample"  [samples]  {:pre [(coll? samples) (not (empty? samples))]}  (let [freqs (frequencies samples)        empir-mean (/ (dot-product (map first freqs) (map second freqs)) (count samples))        sorted-freqs (reverse (sort-by val freqs))        cumu-sums (reductions + (vals sorted-freqs))        normalizer (last cumu-sums)]    (->EmpiricalDistr      (map vector        (keys sorted-freqs)        (map #(/ % normalizer) cumu-sums))      empir-mean)))(defn sample-distr  "Return an infinite lazy seq of random samples from an empirical  discrete probability distribution"  [emp-distr]  (map    (fn [rnd]      (first  ; retrieve 'symbol' from the occurrence        (com/find-first          (fn [[_ cum-prob]] (<= rnd cum-prob))          (:symbcumuprobs emp-distr))))    (repeatedly rand)))