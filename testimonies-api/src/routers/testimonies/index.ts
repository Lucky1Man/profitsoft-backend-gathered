import express from "express";

import {
  saveTestimony,
  getFilteredTestimonies,
  getCounts,
} from "src/controllers/testimonies";

const router = express.Router();

router.post("", saveTestimony);
router.get("", getFilteredTestimonies);
router.post("/_counts", getCounts);

export default router;
