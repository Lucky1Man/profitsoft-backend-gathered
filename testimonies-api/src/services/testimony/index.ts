import { isDate, isFuture } from "date-fns";
import { TestimonyFilterDto } from "src/dto/testimony/testimonyFilterDto";
import { TestimonyGetCountsDto } from "src/dto/testimony/testimonyGetCountsDto";
import { TestimonyGetDto } from "src/dto/testimony/testimonyGetDto";
import { TestimonyRequestCountsDto } from "src/dto/testimony/testimonyRequestCountsDto";
import { TestimonySaveDto } from "src/dto/testimony/testimonySaveDto";
import Testimony, { ITestimony } from "src/model/testimony";
import { InternalError } from "src/system/internalError";
import { validate as validateUuid } from "uuid";
import { getExecutionFactById } from "../execution-fact";
import { getWitnessById } from "../witness";

export const saveTestimony = async (
  saveDto: TestimonySaveDto
): Promise<string> => {
  await validateTestimony(saveDto);
  const testimony = await new Testimony(saveDto).save();
  return testimony.id;
};

export const getFilteredTestimonies = async (filter: TestimonyFilterDto) => {
  validateFilter(filter);
  const { executionFactId } = filter;
  const testimonies = await Testimony.find({
    ...(executionFactId && { executionFactId }),
  })
    .skip(filter.from)
    .limit(filter.size)
    .sort({ timestamp: -1 });
  return testimonies.map((testimony) => toGetDto(testimony));
};

export const getCounts = async (
  requestCounts: TestimonyRequestCountsDto
): Promise<TestimonyGetCountsDto> => {
  const notFoundIds = requestCounts.executionFactIds;
  validateCountsDto(requestCounts);
  const result = await Testimony.aggregate([
    {
      $match: { executionFactId: { $in: requestCounts.executionFactIds } },
    },
    {
      $group: {
        _id: "$executionFactId",
        count: { $sum: 1 },
      },
    },
  ]);
  const mappedData = result.reduce(
    (
      previousValue: TestimonyGetCountsDto,
      currentValue: { _id: string; count: number; }
    ) => {
      notFoundIds.splice(notFoundIds.findIndex(id => id === currentValue._id), 1);
      previousValue[currentValue._id] = currentValue.count;
      return previousValue;
    },
    {}
  );
  notFoundIds.forEach(id => mappedData[id] = 0);
  return mappedData;
};

const toGetDto = (testimony: ITestimony): TestimonyGetDto => {
  return {
    _id: testimony._id,
    witnessId: testimony.witnessId,
    executionFactId: testimony.executionFactId,
    timestamp: testimony.timestamp,
  };
};

const validateTestimony = async (saveDto: TestimonySaveDto) => {
  if (isDate(saveDto.timestamp)) {
    if (isFuture(saveDto.timestamp)) {
      throw new InternalError({
        message: `Timestamp: ${saveDto.timestamp}, can not be in the future.`,
        status: 400,
      });
    }
  } else {
    throw new InternalError({
      message: `Timestamp: ${saveDto.timestamp}, is not a date.`,
      status: 400,
    });
  }
  await validateId(
    saveDto.witnessId,
    async (id) => getWitnessById(id),
    "witness"
  );
  await validateId(
    saveDto.executionFactId,
    async (id) => getExecutionFactById(id),
    "execution fact"
  );
};

const validateId = async (
  id: string,
  fetchFunction: (id: string) => Promise<{ id: string }>,
  validatedEntityName: string
) => {
  if (validateUuid(id)) {
    const response = await fetchFunction(id);
    if (response.id !== id) {
      throw new InternalError({
        message: `Given ${validatedEntityName} id: ${id}, does not exist.`,
        status: 404,
      });
    }
  } else {
    throw new InternalError({
      message: `Given invalid ${validatedEntityName} id: ${id}.`,
      status: 400,
    });
  }
};

const validateFilter = (filter: TestimonyFilterDto) => {
  if (!validateUuid(filter.executionFactId)) {
    throw new InternalError({
      message: `Given invalid execution fact id: ${filter.executionFactId}.`,
      status: 400,
    });
  }
  if (filter.size < 0) {
    throw new InternalError({
      message: `Size must be positive: ${filter.size}.`,
      status: 400,
    });
  }
  if (filter.from < 0) {
    throw new InternalError({
      message: `From must be positive: ${filter.from}.`,
      status: 400,
    });
  }
  if (filter.size > 300) {
    throw new InternalError({
      message: `Size must be less then 300, given size: ${filter.size}.`,
      status: 400,
    });
  }
};

const validateCountsDto = (requestCounts: TestimonyRequestCountsDto) => {
  let fieldPosition = 0;
  requestCounts.executionFactIds.forEach((id) => {
    if (!validateUuid(id)) {
      throw new InternalError({
        message: `Invalid id: ${id}, at position: ${fieldPosition}.`,
        status: 400,
      });
    }
    fieldPosition++;
  });
};
