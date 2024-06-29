import { Request, Response } from "express";
import httpStatus from "http-status";
import log4js from "log4js";
import { ParsedQs } from "qs";
import { TestimonyFilterDto } from "src/dto/testimony/testimonyFilterDto";
import { TestimonyRequestCountsDto } from "src/dto/testimony/testimonyRequestCountsDto";
import { TestimonySaveDto } from "src/dto/testimony/testimonySaveDto";
import {
  getCounts as getCountsApi,
  getFilteredTestimonies as getFilteredTestimoniesApi,
  saveTestimony as saveTestimonyApi,
} from "src/services/testimony";
import { InternalError } from "src/system/internalError";

export const saveTestimony = async (req: Request, res: Response) => {
  try {
    const saveDto = new TestimonySaveDto(req.body);
    const id = await saveTestimonyApi(saveDto);
    res.status(httpStatus.CREATED).send({
      id,
    });
  } catch (err) {
    const { message, status } = new InternalError(err);
    log4js.getLogger().error("Error in creating testimony.", err);
    res.status(status).send({ message });
  }
};

export const getFilteredTestimonies = async (req: Request, res: Response) => {
  try {
    const { executionFactId, from, size } = req.query;
    throwIfRequiredParamsNotPresent(req.query);
    const testimonies = await getFilteredTestimoniesApi(
      new TestimonyFilterDto({
        executionFactId: executionFactId!.toString(),
        from: from !== undefined ? parseInt(from.toString()) : 0,
        size: size !== undefined ? parseInt(size.toString()) : 100,
      })
    );
    res.status(httpStatus.OK).send([
      ...testimonies,
    ]);
  } catch (err) {
    const { message, status } = new InternalError(err);
    log4js.getLogger().error("Error in getting filtered testimonies.", err);
    res.status(status).send({ message });
  }
};

export const getCounts = async (req: Request, res: Response) => {
  try {
    const requestCounts = new TestimonyRequestCountsDto(req.body);
    const counts = await getCountsApi(requestCounts);
    res.status(httpStatus.OK).send({
      ...counts,
    });
  } catch (err) {
    const { message, status } = new InternalError(err);
    log4js.getLogger().error("Error in getting counts.", err);
    res.status(status).send({ message });
  }
};

function throwIfRequiredParamsNotPresent(query: ParsedQs) {
  if (!query.executionFactId) {
    throw new InternalError({
      message: "Execution fact id query parameter is required.",
      status: 400,
    });
  }
}
